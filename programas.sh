#!/usr/bin/env bash
# Instala SEM Flatpak, cada um da fonte oficial:
#   Zen (tarball oficial), Spotify (repositório apt oficial), ncspot (release oficial do GitHub),
#   Discord (.deb oficial) e Prism Launcher (AppImage oficial).
# Uso: bash programas.sh
set -euo pipefail

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo ">> Dependências..."
sudo apt update
sudo apt install -y curl gpg xz-utils python3 libfuse2t64 \
  libncursesw6 libdbus-1-3 libpulse0 libxcb-shape0 libxcb-xfixes0

mkdir -p ~/.local/share/applications

# ---------------- ZEN (tarball oficial) ----------------
echo ">> Zen Browser..."
curl -fL https://github.com/zen-browser/desktop/releases/latest/download/zen.linux-x86_64.tar.xz -o "$TMP/zen.tar.xz"
sudo rm -rf /opt/zen
sudo tar -xJf "$TMP/zen.tar.xz" -C /opt
sudo ln -sf /opt/zen/zen /usr/local/bin/zen
cat > ~/.local/share/applications/zen.desktop <<'EOF'
[Desktop Entry]
Name=Zen Browser
Exec=/opt/zen/zen %u
Icon=/opt/zen/browser/chrome/icons/default/default128.png
Type=Application
Categories=Network;WebBrowser;
MimeType=text/html;x-scheme-handler/http;x-scheme-handler/https;
EOF

# ---------------- SPOTIFY (repositório apt oficial) ----------------
echo ">> Spotify..."
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.spotify.com/debian/pubkey_5384CE82BA52C83A.asc \
  | sudo gpg --dearmor --yes -o /etc/apt/keyrings/spotify.gpg
echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/spotify.gpg] http://repository.spotify.com stable non-free" \
  | sudo tee /etc/apt/sources.list.d/spotify.list >/dev/null
sudo apt update
sudo apt install -y spotify-client

# ---------------- NCSPOT (binário oficial do GitHub) ----------------
echo ">> ncspot (Spotify no terminal)..."
NCSPOT_URL=$(curl -fsSL https://api.github.com/repos/hrkfdn/ncspot/releases/latest | python3 -I -c '
import json, sys
assets = json.load(sys.stdin)["assets"]
for a in assets:
    n = a["name"].lower()
    if "linux" in n and "x86_64" in n and n.endswith(".tar.gz"):
        print(a["browser_download_url"]); break
')
if [ -n "$NCSPOT_URL" ]; then
  curl -fL "$NCSPOT_URL" -o "$TMP/ncspot.tar.gz"
  mkdir -p "$TMP/ncspot" && tar -xzf "$TMP/ncspot.tar.gz" -C "$TMP/ncspot"
  sudo install -m 755 "$(find "$TMP/ncspot" -type f -name ncspot | head -n1)" /usr/local/bin/ncspot
else
  echo "!! Não achei o binário do ncspot para Linux no GitHub. Pulei essa parte."
fi

# ---------------- DISCORD (.deb oficial) ----------------
echo ">> Discord..."
curl -fL "https://discord.com/api/download?platform=linux&format=deb" -o "$TMP/discord.deb"
sudo apt install -y "$TMP/discord.deb"

# ---------------- PRISM LAUNCHER (AppImage oficial) ----------------
echo ">> Prism Launcher..."
sudo mkdir -p /opt/prismlauncher
sudo curl -fL https://github.com/PrismLauncher/PrismLauncher/releases/latest/download/PrismLauncher-Linux-x86_64.AppImage \
  -o /opt/prismlauncher/PrismLauncher.AppImage
sudo chmod +x /opt/prismlauncher/PrismLauncher.AppImage
sudo ln -sf /opt/prismlauncher/PrismLauncher.AppImage /usr/local/bin/prismlauncher
cat > ~/.local/share/applications/prismlauncher.desktop <<'EOF'
[Desktop Entry]
Name=Prism Launcher
Exec=/opt/prismlauncher/PrismLauncher.AppImage
Type=Application
Categories=Game;
EOF

# remove o alias antigo do Flatpak, se existir
sed -i "/alias ncspot='flatpak run/d" ~/.bashrc 2>/dev/null || true

echo
echo ">> Pronto! Abra pelo Alt+D: Zen Browser, Spotify, Discord, Prism Launcher"
echo "   Spotify no terminal: ncspot"
