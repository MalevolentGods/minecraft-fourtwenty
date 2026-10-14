#!/usr/bin/env python3
"""Freeze, verify and prepare isolated compatibility runs. Never accesses player saves."""
import argparse
import hashlib
import io
import json
import platform
import shutil
import subprocess
import tarfile
import tomllib
import zipfile
import re
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / 'validation/compatibility-lock.json'
DEFAULT_MODS = Path('/Users/oberon/Documents/curseforge/minecraft/Instances/MC-TechColonies/mods')
SIBLING = ROOT.parent / 'minecolonies-cult'
UNDER_TEST = {'fourtwenty', 'minecolonies_cult'}
COPY_DIRS = ('config', 'defaultconfigs', 'datapacks', 'global_packs', 'resourcepacks')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_manifest(data):
    """Unfold the JAR manifest before resolving loader version substitutions."""
    result = {}
    lines = data.decode().replace('\r\n', '\n').replace('\n ', '').splitlines()
    for line in lines:
        if ': ' in line:
            key, value = line.split(': ', 1)
            result[key] = value
    return result


def inspect_jar(data, name):
    """Record both loader metadata and all declared nested artifacts, recursively."""
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        names = jar.namelist()
        manifest = read_manifest(jar.read('META-INF/MANIFEST.MF')) if 'META-INF/MANIFEST.MF' in names else {}
        metadata = next((n for n in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml') if n in names), None)
        mod_data = tomllib.loads(jar.read(metadata).decode()) if metadata else {}
        mods = []
        for mod in mod_data.get('mods', []):
            version = str(mod.get('version', ''))
            if version == '${file.jarVersion}':
                version = manifest.get('Implementation-Version', '')
            if not version or '${' in version:
                raise ValueError(f'Unresolved version in {name}: {version!r}')
            mods.append({'id': mod['modId'], 'version': version})
        nested = []
        if 'META-INF/jarjar/metadata.json' in names:
            for entry in json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']:
                nested.append({'declaration': entry, 'artifact': inspect_jar(jar.read(entry['path']), entry['path'])})
        return {'file': name, 'sha256': digest(data), 'mods': mods,
                'loader': mod_data.get('modLoader'), 'loaderVersion': mod_data.get('loaderVersion'),
                'dependencies': mod_data.get('dependencies', {}), 'nested': nested}


def inventory(mods):
    result = [inspect_jar(p.read_bytes(), p.name) for p in sorted(mods.glob('*.jar'))]
    seen = {}
    for artifact in result:
        for mod in artifact['mods']:
            if mod['id'] in seen:
                raise ValueError(f'Duplicate mod ID {mod["id"]}: {seen[mod["id"]]} and {artifact["file"]}')
            seen[mod['id']] = artifact['file']
    if not result:
        raise ValueError(f'No mod JARs found in {mods}')
    return result


def revision(path):
    return subprocess.check_output(['git', '-C', str(path), 'rev-parse', 'HEAD'], text=True).strip()


def snapshot(mods):
    """An exclusive initial snapshot; target changes require an explicit reviewed edit."""
    data = {'schema': 1, 'minecraft': '1.21.1', 'neoforge': '21.1.218', 'java': 21,
            'sourceDirectory': str(mods.resolve()), 'platform': platform.platform(),
            'sources': {'fourtwenty': revision(ROOT), 'minecolonies_cult': revision(SIBLING)},
            'core': json.loads((SIBLING / 'reference-lock.json').read_text())['artifacts'],
            'artifacts': inventory(mods)}
    with LOCK.open('x') as stream:
        json.dump(data, stream, indent=2)
        stream.write('\n')
    print(f'Frozen {len(data["artifacts"])} JARs in {LOCK}')


def verify(mods):
    lock = json.loads(LOCK.read_text())
    if inventory(mods) != lock['artifacts']:
        raise ValueError('Reference inventory changed; review the lock explicitly. No files copied.')
    for entry in lock['core']:
        if digest((mods / entry['file']).read_bytes()) != entry['sha256']:
            raise ValueError(f'Core checksum mismatch: {entry["file"]}')
    return lock


def files_below(path):
    return {str(p.relative_to(path)): digest(p.read_bytes()) for p in sorted(path.rglob('*')) if p.is_file()}


def build_cult(mods, destination, gradle_home):
    """Build the pinned commit in an exclusive archive checkout, never the sibling tree."""
    lock = verify(mods)
    if destination.exists():
        raise ValueError('Cult checkout destination already exists; use a fresh directory.')
    archive = subprocess.check_output(['git', '-C', str(SIBLING), 'archive', lock['sources']['minecolonies_cult']])
    destination.mkdir(parents=True)
    with tarfile.open(fileobj=io.BytesIO(archive)) as tf:
        tf.extractall(destination, filter='data')
    (destination / 'libs').mkdir(exist_ok=True)
    for entry in lock['core']:
        shutil.copy2(mods / entry['file'], destination / 'libs' / entry['file'])
    import os
    env = dict(os.environ, GRADLE_USER_HOME=str(gradle_home.resolve()))
    result = subprocess.run(['bash', './gradlew', 'build', '--console=plain'], cwd=destination, env=env)
    if result.returncode:
        raise ValueError('Isolated cult build failed; retain checkout/logs for diagnosis.')
    jars = [p for p in (destination / 'build/libs').glob('*.jar') if not p.name.endswith('-sources.jar')]
    if len(jars) != 1:
        raise ValueError(f'Expected one cult artifact, found {jars}')
    record = {'revision': lock['sources']['minecolonies_cult'], 'artifact': inspect_jar(jars[0].read_bytes(), jars[0].name)}
    (destination / 'build-record.json').write_text(json.dumps(record, indent=2) + '\n')
    print(jars[0])


def prepare(mods, destination, profile, cult_jar, fourtwenty_jar, headless, exclusions=None):
    lock = verify(mods)
    if destination.exists():
        raise ValueError('Run destination already exists; create a fresh run, never merge stale mods.')
    if headless and profile not in ('full', 'baseline'):
        raise ValueError('Headless adjustments only apply to full/baseline GameTests.')
    exclusions = exclusions or []
    if exclusions and not headless:
        raise ValueError('Diagnostic exclusions require a headless profile; original-settings qualification must keep the full inventory.')
    excluded = {}
    for entry in exclusions:
        mod_id, separator, reason = entry.partition('=')
        if not separator or not reason.strip() or mod_id in UNDER_TEST:
            raise ValueError('Exclusion must be mod_id=reason and may not remove either mod under test.')
        if not any(mod_id == m['id'] for a in lock['artifacts'] for m in a['mods']):
            raise ValueError(f'Unknown excluded mod: {mod_id}')
        excluded[mod_id] = reason
    selected = []
    if profile != 'standalone':
        core_files = {a['file'] for a in lock['core']}
        for artifact in lock['artifacts']:
            if UNDER_TEST.intersection(m['id'] for m in artifact['mods']):
                continue
            if excluded.keys() & {m['id'] for m in artifact['mods']}:
                continue
            if profile in ('full', 'baseline') or artifact['file'] in core_files:
                selected.append(mods / artifact['file'])
        if cult_jar is None:
            raise ValueError('A freshly built --cult-jar is required.')
        build_record = cult_jar.parent.parent.parent / 'build-record.json'
        record = json.loads(build_record.read_text())
        if record['revision'] != lock['sources']['minecolonies_cult'] or record['artifact']['sha256'] != digest(cult_jar.read_bytes()):
            raise ValueError('Cult build provenance does not match the frozen revision/artifact.')
        selected.append(cult_jar)
    if fourtwenty_jar and profile != 'baseline':
        artifact = inspect_jar(fourtwenty_jar.read_bytes(), fourtwenty_jar.name)
        if artifact['mods'] != [{'id': 'fourtwenty', 'version': '0.3.2'}]:
            raise ValueError('Unexpected FourTwenty release metadata.')
        selected.append(fourtwenty_jar)
    # Preflight every selected artifact and duplicate ID before any copy occurs.
    seen = set()
    for path in selected:
        for mod in inspect_jar(path.read_bytes(), path.name)['mods']:
            if mod['id'] in seen:
                raise ValueError(f'Duplicate selected mod ID: {mod["id"]}')
            seen.add(mod['id'])
    destination.mkdir(parents=True)
    (destination / 'mods').mkdir()
    for path in selected:
        shutil.copy2(path, destination / 'mods' / path.name)
    copied = []
    if profile in ('full', 'baseline'):
        for name in COPY_DIRS:
            source = mods.parent / name
            if source.is_dir():
                shutil.copytree(source, destination / name)
                copied.append(name)
    overrides = {}
    if headless:
        overrides = {'betterchunkloading.json': {key: False for key in ('enablePrediction', 'enableSmartChunkLoading', 'enableSmartPostProcessing', 'optimizeWaiting')},
                     'limitedchunks.json': {'excludedtickets': ['forced', 'gametest', 'start', 'unknown']}}
        for name, changes in overrides.items():
            path = destination / 'config' / name
            data = json.loads(path.read_text())
            for key, value in changes.items():
                data[key][key] = value
            path.write_text(json.dumps(data, indent=2) + '\n')
    resource_selection = []
    options = mods.parent / 'options.txt'
    if options.exists():
        resource_selection = [line for line in options.read_text().splitlines() if line.startswith(('resourcePacks:', 'incompatibleResourcePacks:'))]
        # These are the only copied client options; no account/server/history data.
        (destination / 'options.txt').write_text('\n'.join(resource_selection) + '\n')
    record = {'minecraft': lock['minecraft'], 'neoforge': lock['neoforge'], 'java': lock['java'],
              'profile': profile, 'packaged': bool(fourtwenty_jar) or profile == 'baseline',
              'sources': lock['sources'], 'mods': inventory(destination / 'mods') if selected else [],
              'configuration': {name: files_below(destination / name) for name in copied},
              'headlessOverrides': overrides, 'resourcePackSelection': resource_selection,
              'serverExclusions': excluded, 'qualification': 'diagnostic-only' if excluded else 'candidate'}
    (destination / 'compatibility-run.json').write_text(json.dumps(record, indent=2) + '\n')
    # Compile-only fixtures use a stable verified dependency copy, not the installation.
    deps = ROOT / '.compat/dependencies'
    deps.mkdir(parents=True, exist_ok=True)
    fixture_ids = {'actuallyharvest', 'playerrevive', 'creativecore', 'whitenoise'}
    fixture_deps = lock['core'] + [a for a in lock['artifacts'] if fixture_ids.intersection(m['id'] for m in a['mods'])]
    for entry in fixture_deps:
        target = deps / entry['file']
        if target.exists() and digest(target.read_bytes()) != entry['sha256']:
            raise ValueError(f'Changed fixture dependency: {target}')
        if not target.exists(): shutil.copy2(mods / entry['file'], target)
    print(f'Prepared {profile}: {len(selected)} JARs at {destination}')


def verify_run(destination, profile, packaged):
    record = json.loads((destination / 'compatibility-run.json').read_text())
    if record['profile'] != profile or record['packaged'] != packaged:
        raise ValueError('Launch profile/packaged mode differs from preparation.')
    actual = inventory(destination / 'mods') if list((destination / 'mods').glob('*.jar')) else []
    if actual != record['mods']:
        raise ValueError('Prepared JAR inventory changed; refusing stale/duplicate test artifacts.')
    print(f'Run inventory verified: {profile}, {len(actual)} JARs')


def record_launch(destination):
    record = json.loads((destination / 'compatibility-run.json').read_text())
    verify_run(destination, record['profile'], record['packaged'])
    launch = {'startedAt': datetime.now(timezone.utc).isoformat(), 'revision': revision(ROOT),
              'sourceFiles': files_below(ROOT / 'src'),
              'compiledClasses': files_below(ROOT / 'build/classes/java'),
              'compiledResources': files_below(ROOT / 'build/resources'),
              'configurationBefore': {name: files_below(destination / name) for name in COPY_DIRS if (destination / name).exists()}}
    (destination / 'compatibility-launch.json').write_text(json.dumps(launch, indent=2) + '\n')


def capture(destination, output, command):
    """Preserve a run's inputs, outputs and config drift without overwriting earlier evidence."""
    if output.exists():
        raise ValueError('Evidence destination exists; use a new record.')
    record = json.loads((destination / 'compatibility-run.json').read_text())
    verify_run(destination, record['profile'], record['packaged'])
    output.mkdir(parents=True)
    shutil.copy2(destination / 'compatibility-run.json', output / 'inputs.json')
    if (destination / 'compatibility-launch.json').exists():
        shutil.copy2(destination / 'compatibility-launch.json', output / 'launch.json')
    native_log = destination / 'logs/latest.log'
    log = native_log.read_text() if native_log.exists() else ''
    if native_log.exists(): shutil.copy2(native_log, output / 'native.log')
    pass_match = re.search(r'All ([1-9][0-9]*) required tests passed', log)
    result = {'capturedAt': datetime.now(timezone.utc).isoformat(), 'command': command,
              'requiredGameTestsPassed': int(pass_match[1]) if pass_match else None,
              'failedScenarios': [line for line in log.splitlines() if ' failed at ' in line],
              'qualification': record.get('qualification', 'candidate'),
              'configurationAfter': {name: files_below(destination / name) for name in COPY_DIRS if (destination / name).exists()},
              'currentRevision': revision(ROOT),
              'sourceFilesAtCapture': {str(p.relative_to(ROOT)): digest(p.read_bytes())
                              for p in sorted((ROOT / 'src').rglob('*')) if p.is_file()},
              'logsSha256': digest(log.encode())}
    (output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    (output / 'warnings.txt').write_text('\n'.join(line for line in log.splitlines() if '/WARN]' in line or '/ERROR]' in line) + '\n')
    print(f'Captured evidence: {output}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('snapshot', 'verify', 'build-cult', 'prepare', 'verify-run', 'record-launch', 'capture'))
    parser.add_argument('--mods', type=Path, default=DEFAULT_MODS)
    parser.add_argument('--destination', type=Path)
    parser.add_argument('--profile', choices=('standalone', 'colony', 'full', 'baseline'), default='standalone')
    parser.add_argument('--cult-jar', type=Path)
    parser.add_argument('--fourtwenty-jar', type=Path)
    parser.add_argument('--headless-adjustments', action='store_true')
    parser.add_argument('--exclude-diagnostic-mod', action='append', default=[], metavar='ID=REASON')
    parser.add_argument('--packaged', choices=('true', 'false'), default='false')
    parser.add_argument('--gradle-home', type=Path, default=ROOT / '.gradle-home')
    parser.add_argument('--output', type=Path)
    parser.add_argument('--command', default='')
    args = parser.parse_args()
    try:
        if args.action == 'snapshot': snapshot(args.mods)
        elif args.action == 'verify':
            print(f'Verified {len(verify(args.mods)["artifacts"])} reference JARs')
        elif args.destination is None: parser.error('--destination is required')
        elif args.action == 'build-cult': build_cult(args.mods, args.destination, args.gradle_home)
        elif args.action == 'record-launch': record_launch(args.destination)
        elif args.action == 'capture':
            if args.output is None or not args.command: parser.error('capture requires --output and --command')
            capture(args.destination, args.output, args.command)
        elif args.action == 'prepare': prepare(args.mods, args.destination, args.profile, args.cult_jar, args.fourtwenty_jar, args.headless_adjustments, args.exclude_diagnostic_mod)
        else: verify_run(args.destination, args.profile, args.packaged == 'true')
    except (ValueError, OSError) as error:
        parser.exit(1, f'{error}\n')


if __name__ == '__main__':
    main()
