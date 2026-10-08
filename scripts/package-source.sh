#!/bin/bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
mkdir -p out
git archive --format=tar HEAD | gzip -n > out/sharkdroid-source.tar.gz
ls -la out/sharkdroid-source.tar.gz
