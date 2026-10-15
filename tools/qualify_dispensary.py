#!/usr/bin/env python3
"""Run and capture one isolated packaged qualification scenario; never replace an installation."""
import argparse
import json
import os
import signal
import shutil
import subprocess
import threading
import time
from pathlib import Path
import compatibility as compat


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('kind', choices=('gametest', 'client', 'server'))
    parser.add_argument('--name', required=True, help='Fresh evidence name, or a new name for a reused checkpoint')
    parser.add_argument('--profile', choices=('standalone', 'colony', 'full', 'baseline'), default='colony')
    parser.add_argument('--mods', type=Path, required=True, help='Exact verified reference inventory; parent supplies pack settings')
    parser.add_argument('--cult-jar', type=Path)
    parser.add_argument('--jar', type=Path, default=Path('build/libs/fourtwenty-0.3.2.jar'))
    parser.add_argument('--without-cult', action='store_true')
    parser.add_argument('--headless-adjustments', action='store_true')
    parser.add_argument('--exclude-diagnostic-mod', action='append', default=[])
    parser.add_argument('--reuse', type=Path, help='Explicit previously prepared checkpoint; inventory must still match')
    parser.add_argument('--world', type=Path, help='Disposable GameTest world to copy into the client profile')
    parser.add_argument('--accept-eula', action='store_true', help='Server owner explicitly accepts the Minecraft EULA for this test')
    parser.add_argument('--timeout', type=int, default=360)
    parser.add_argument('--gradle-arg', action='append', default=[])
    args = parser.parse_args()
    if Path(args.name).name != args.name or args.name in ('.', '..'):
        parser.error('name must be a single directory name')
    if args.kind == 'server' and not args.accept_eula:
        parser.error('ordinary server requires explicit owner authorization via --accept-eula')
    if args.kind != 'gametest' and (args.headless_adjustments or args.exclude_diagnostic_mod):
        parser.error('client and ordinary server qualification must retain original pack settings')
    if args.world and (args.kind != 'client' or not args.world.is_dir()):
        parser.error('--world requires a client run and an existing disposable world directory')
    evidence = compat.ROOT / 'validation/evidence/dispensary-phase6' / args.name
    console = evidence.with_suffix('.log')
    if evidence.exists() or console.exists():
        parser.error('evidence already exists; choose a new name')
    run = args.reuse or compat.ROOT / '.compat/runs' / ('dispensary-phase6-' + args.name)
    if args.reuse:
        compat.verify_run(run, args.profile, True)
        previous = json.loads((run / 'compatibility-run.json').read_text())
        if args.kind != 'gametest' and (previous.get('headlessOverrides') or previous.get('serverExclusions')):
            parser.error('cannot reuse a diagnostic/headless profile for original-settings client/server qualification')
    else:
        compat.prepare(args.mods, run, args.profile, args.cult_jar, args.jar, args.headless_adjustments,
                       args.exclude_diagnostic_mod, args.without_cult)
    if args.world:
        shutil.copytree(args.world, run / 'saves/benefits')
    if args.kind == 'client':
        # Skip only the first-launch accessibility prompt; preserve copied resource-pack selection.
        options = run / 'options.txt'
        existing = options.read_text() if options.exists() else ''
        options.write_text(existing + '\nonboardAccessibility:false\nlang:en_us\n')
    if args.kind == 'server':
        (run / 'eula.txt').write_text('# Authorized by server owner for disposable qualification\neula=true\n')
        # Local-only socket; never expose this disposable world on other interfaces.
        properties = run / 'server.properties'
        settings = properties.read_text() if properties.exists() else 'online-mode=true\nlevel-seed=420\nview-distance=5\nsimulation-distance=5\n'
        settings = '\n'.join(line for line in settings.splitlines() if not line.startswith(('server-ip=', 'server-port=')))
        properties.write_text(settings + '\nserver-ip=127.0.0.1\nserver-port=25576\n')
    command = ['./gradlew', {'server': 'runServer', 'client': 'runClient', 'gametest': 'runGameTestServer'}[args.kind],
               '-Ppackaged=true', '-PcompatProfile=' + args.profile, '-PcompatRunDir=' + str(run), *args.gradle_arg]
    evidence.parent.mkdir(parents=True, exist_ok=True)
    started = threading.Event()
    lines = []
    begin = time.monotonic()
    print('Starting', args.name, flush=True)
    with console.open('x') as stream:
        process = subprocess.Popen(command, cwd=compat.ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
        def output():
            for line in process.stdout:
                stream.write(line); stream.flush(); lines.append(line)
                if 'Done (' in line and 'For help' in line: started.set()
        reader = threading.Thread(target=output, daemon=True); reader.start()
        timed_out = False
        if args.kind == 'server':
            while not started.is_set() and process.poll() is None and time.monotonic() - begin < args.timeout:
                started.wait(.25)
            if started.is_set():
                # Observe a short idle window, then request a normal world save/shutdown.
                time.sleep(10)
                process.stdin.write('stop\n'); process.stdin.flush()
        try:
            process.wait(timeout=max(1, args.timeout - (time.monotonic() - begin)))
        except subprocess.TimeoutExpired:
            timed_out = True
            os.killpg(process.pid, signal.SIGTERM)
            try: process.wait(timeout=20)
            except subprocess.TimeoutExpired: os.killpg(process.pid, signal.SIGKILL); process.wait()
        reader.join(timeout=10)
    import shlex
    compat.capture(run, evidence, shlex.join(command))
    if (run / 'server.properties').exists(): shutil.copy2(run / 'server.properties', evidence / 'server.properties')
    log = ''.join(lines)
    if args.kind == 'server':
        passed = started.is_set() and 'All dimensions are saved' in log and process.returncode == 0
    elif args.kind == 'client':
        passed = any(marker in log for marker in ('FOURTWENTY_PRESENTATION_CLIENT_PROBE_PASSED',
                                                   'FOURTWENTY_STANDALONE_CLIENT_PROBE_PASSED')) and '_PROBE_FAILED' not in log and process.returncode == 0
    else:
        passed = 'required tests passed' in log and process.returncode == 0
        for flag in args.gradle_arg:
            if flag.startswith('-PdispensaryLifecycleStage='):
                passed &= 'FOURTWENTY_COLONY_CHECKPOINT_' + flag.split('=')[1].upper() in log
    result = {'kind': args.kind, 'exitCode': process.returncode, 'timedOut': timed_out, 'passed': passed and not timed_out,
              'durationSeconds': round(time.monotonic() - begin, 2), 'run': str(run),
              'scope': 'startup, ten-second idle and orderly shutdown; no connected gameplay' if args.kind == 'server' else 'automated fixture/probe',
              'ownerAcceptedEula': args.accept_eula if args.kind == 'server' else None}
    (evidence / 'qualification.json').write_text(json.dumps(result, indent=2) + '\n')
    if (run / 'screenshots').exists(): shutil.copytree(run / 'screenshots', evidence / 'screenshots')
    print(json.dumps(result), flush=True)
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
