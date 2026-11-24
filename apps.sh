#!/usr/bin/env bash
# Ativa os repositórios contrib e non-free no Debian 13 (Trixie).
# Uso: bash apps.sh
set -euo pipefail

sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak 2>/dev/null || true
sudo tee /etc/apt/sources.list >/dev/null <<'EOF'
deb http://deb.debian.org/debian trixie main contrib non-free non-free-firmware
deb http://deb.debian.org/debian trixie-updates main contrib non-free non-free-firmware
deb http://security.debian.org/debian-security trixie-security main contrib non-free non-free-firmware
EOF
sudo apt update

echo ">> contrib e non-free ativados."
