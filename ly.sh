#!/usr/bin/env bash
# Instala o Ly (tela de login no terminal) no Debian 13 e ativa no tty2.
# Uso: bash ly.sh
set -euo pipefail

sudo apt update
sudo apt install -y build-essential git curl python3 libpam0g-dev libxcb-xkb-dev xauth brightnessctl

# ---------------- LY (tela de login no terminal) ----------------
echo ">> Instalando o Ly..."
LY_DIR=/tmp/ly-build
rm -rf "$LY_DIR" && mkdir -p "$LY_DIR"
git clone https://codeberg.org/fairyglade/ly "$LY_DIR/ly"
cd "$LY_DIR/ly"
# usa a última versão estável (tag), não o código em desenvolvimento
LY_TAG=$(git describe --tags "$(git rev-list --tags --max-count=1)")
git checkout -q "$LY_TAG"
echo "   Ly $LY_TAG"

# descobre qual Zig o Ly pede e baixa a versão estável mais nova dessa série
ZIG_MIN=$(grep -oP 'minimum_zig_version\s*=\s*"\K[0-9.]+' build.zig.zon || true)
if [ -z "$ZIG_MIN" ]; then
  echo "!! Não consegui descobrir a versão do Zig. O LightDM continua como tela de login."
  exit 1
fi
ZIG_SERIES=$(echo "$ZIG_MIN" | cut -d. -f1,2)
curl -fsSL https://ziglang.org/download/index.json -o "$LY_DIR/zig-index.json"
ZIG_URL=$(python3 -I - "$LY_DIR/zig-index.json" "$ZIG_SERIES" <<'PYEOF'
import json, sys
data = json.load(open(sys.argv[1])); series = sys.argv[2]
vers = [v for v in data if v.startswith(series + ".") and "x86_64-linux" in data[v]]
vers.sort(key=lambda v: [int(x) for x in v.split(".")])
print(data[vers[-1]]["x86_64-linux"]["tarball"])
PYEOF
)
echo "   Zig: $ZIG_URL"
# o servidor oficial costuma ser lento: tenta os mirrors da comunidade primeiro
ZIG_FILE=$(basename "$ZIG_URL")
ZIG_OK=0
for m in $(curl -fsSL https://ziglang.org/download/community-mirrors.txt 2>/dev/null | shuf) ; do
  echo "   tentando mirror: $m"
  if curl -fL --connect-timeout 10 --speed-limit 200000 --speed-time 15 \
       "$m/$ZIG_FILE" -o "$LY_DIR/zig.tar.xz"; then
    ZIG_OK=1; break
  fi
done
if [ "$ZIG_OK" = 0 ]; then
  echo "   mirrors falharam, usando o servidor oficial (pode demorar)..."
  curl -fL "$ZIG_URL" -o "$LY_DIR/zig.tar.xz"
fi
mkdir -p "$LY_DIR/zig" && tar -xf "$LY_DIR/zig.tar.xz" -C "$LY_DIR/zig" --strip-components=1

sudo "$LY_DIR/zig/zig" build installexe -Dinit_system=systemd
cd ~

# só troca a tela de login depois que o Ly compilou com sucesso
for dm in lightdm gdm3 sddm; do
  sudo systemctl disable "$dm.service" 2>/dev/null || true
done
sudo systemctl daemon-reload
sudo systemctl disable getty@tty2.service || true
if [ -f /etc/systemd/system/ly@.service ] || [ -f /usr/lib/systemd/system/ly@.service ] || systemctl list-unit-files 'ly@*' | grep -q 'ly@'; then
  sudo systemctl enable ly@tty2.service
else
  sudo systemctl enable ly.service
fi

echo
echo ">> Ly instalado! Reinicie com: sudo reboot"
