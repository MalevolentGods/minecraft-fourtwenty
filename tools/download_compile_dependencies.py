#!/usr/bin/env python3
"""Download the frozen compile APIs without requiring an installed modpack."""
import argparse
import hashlib
import json
import shutil
import tempfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def download_dependencies(artifacts, urls, destination):
    """Verify the complete download set before changing local compile inputs."""
    with tempfile.TemporaryDirectory(prefix='fourtwenty-compile-') as directory:
        verified = []
        for artifact in artifacts:
            source = Path(directory) / artifact['file']
            request = urllib.request.Request(
                urls[artifact['file']], headers={'User-Agent': 'fourtwenty-build/1.0'})
            with urllib.request.urlopen(request, timeout=120) as response, source.open('wb') as output:
                shutil.copyfileobj(response, output)
            with source.open('rb') as stream:
                actual = hashlib.file_digest(stream, 'sha256').hexdigest()
            if actual != artifact['sha256']:
                raise ValueError(f"Checksum mismatch for {source.name}: {actual}; compile inputs were not changed.")
            verified.append(source)
        destination.mkdir(parents=True, exist_ok=True)
        for source in verified:
            shutil.copy2(source, destination / source.name)
            print(f'Verified {source.name}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--destination', type=Path, default=ROOT / '.compat/dependencies')
    args = parser.parse_args()
    lock = json.loads((ROOT / 'validation/compatibility-lock.json').read_text())
    artifacts = lock['core'] + lock.get('compileApis', [])
    urls = json.loads((ROOT / 'tools/compile-dependency-downloads.json').read_text())
    download_dependencies(artifacts, urls, args.destination)


if __name__ == '__main__':
    main()
