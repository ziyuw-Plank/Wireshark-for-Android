#!/usr/bin/env python3
"""Fetch SHA256-pinned native sources and apply the repository's patches."""
import argparse
import hashlib
import pathlib
import re
import subprocess
import tarfile
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent


def manifest():
    text = (ROOT / "SOURCES.txt").read_text()
    urls = dict(re.findall(r"^(\S+\.tar\.(?:xz|gz|bz2))\s+(https://\S+)", text, re.M))
    hashes = {name: digest for digest, name in
              re.findall(r"^([0-9a-f]{64})\s+(\S+)$", text, re.M)}
    if len(urls) != 9 or any(name not in hashes for name in urls):
        raise ValueError("Expected nine source archives with HTTPS URLs and SHA256 pins")
    return [(name, url, hashes[name]) for name, url in urls.items()]


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate the manifest without downloads")
    parser.add_argument("--offline", action="store_true", help="require existing archives in dl/")
    parser.add_argument("--discard-archives", action="store_true", help="remove verified archives before F-Droid scanning")
    args = parser.parse_args()
    sources = manifest()
    if args.check:
        for name, _, expected in sources:
            print(f"{expected}  {name}")
        return
    downloads, source_dir = ROOT / "dl", ROOT / "src"
    downloads.mkdir(exist_ok=True)
    source_dir.mkdir(exist_ok=True)
    for name, url, expected in sources:
        archive = downloads / name
        if not archive.exists():
            if args.offline:
                raise FileNotFoundError(archive)
            partial = archive.with_name(name + ".part")
            request = urllib.request.Request(url, headers={"User-Agent": "SharkDroid-source-build"})
            with urllib.request.urlopen(request, timeout=120) as response, partial.open("wb") as output:
                if not response.geturl().startswith("https://"):
                    raise ValueError("Source download redirected away from HTTPS")
                for chunk in iter(lambda: response.read(1024 * 1024), b""):
                    output.write(chunk)
            partial.replace(archive)
        if digest(archive) != expected:
            raise ValueError(f"SHA256 mismatch: {name}")
        top = re.sub(r"\.tar\.(xz|gz|bz2)$", "", name)
        destination = source_dir / top
        marker = destination / ".sharkdroid-prepared"
        if destination.exists():
            if not marker.exists() or marker.read_text().strip() != expected:
                raise ValueError(f"Unmanaged source directory: {destination}; use a clean checkout")
        else:
            with tarfile.open(archive) as tar:
                if any(pathlib.PurePosixPath(member.name).parts[0] != top for member in tar.getmembers()):
                    raise ValueError(f"Unexpected archive root: {name}")
                tar.extractall(source_dir, filter="data")
            patches = []
            if top.startswith("wireshark-"):
                patches = ["wireshark-0001-lemon-host-template.patch",
                           "wireshark-0002-bionic-net-if-include.patch"]
            elif top.startswith("libpcap-"):
                patches = ["libpcap-0001-arphrd-rawip-dlt-raw.patch"]
            for patch in patches:
                subprocess.run(["patch", "--batch", "-p1", "-i", str(ROOT / "patches" / patch)],
                               cwd=destination, check=True)
            marker.write_text(expected + "\n")
        print(f"Prepared {top}", flush=True)
        if args.discard_archives:
            archive.unlink()


if __name__ == "__main__":
    main()
