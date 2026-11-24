#!/usr/bin/env bash
# Instala tudo de uma vez: repositórios, drivers AMD, Steam, Flatpak, Discord, Zen, ncspot e corrige o mouse.
# Uso: bash apps.sh   (com seu usuário normal; pede a senha do sudo)
set -euo pipefail

echo ">> Ativando contrib e non-free..."
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak 2>/dev/null || true
sudo tee /etc/apt/sources.list >/dev/null <<'EOF'
deb http://deb.debian.org/debian trixie main contrib non-free non-free-firmware
deb http://deb.debian.org/debian trixie-updates main contrib non-free non-free-firmware
deb http://security.debian.org/debian-security trixie-security main contrib non-free non-free-firmware
EOF
sudo dpkg --add-architecture i386
sudo apt update

echo ">> Drivers AMD (firmware, Mesa, Vulkan 64 e 32 bits)..."
sudo apt install -y firmware-amd-graphics mesa-vulkan-drivers libgl1-mesa-dri vulkan-tools \
  mesa-vulkan-drivers:i386 libgl1-mesa-dri:i386

echo ">> Steam..."
echo steam steam/question select "I AGREE" | sudo debconf-set-selections
echo steam steam/license note '' | sudo debconf-set-selections
sudo DEBIAN_FRONTEND=noninteractive apt install -y steam-installer

echo ">> Flatpak + Flathub..."
sudo apt install -y flatpak
sudo flatpak remote-add --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo

echo ">> Discord, Zen e ncspot (Spotify no terminal)..."
sudo flatpak install -y flathub com.discordapp.Discord app.zen_browser.zen io.github.hrkfdn.ncspot

# atalho "spotify" no terminal
if ! grep -q 'alias spotify=' ~/.bashrc; then
  echo "alias spotify='flatpak run io.github.hrkfdn.ncspot'" >> ~/.bashrc
fi

echo ">> Mouse sem aceleração..."
sudo mkdir -p /etc/X11/xorg.conf.d
sudo tee /etc/X11/xorg.conf.d/50-mouse.conf >/dev/null <<'EOF'
Section "InputClass"
    Identifier "mouse sem aceleracao"
    MatchIsPointer "yes"
    Driver "libinput"
    Option "AccelProfile" "flat"
    Option "AccelSpeed" "0"
EndSection
EOF

echo ">> Picom sem vsync (menos input lag)..."
[ -f ~/.config/picom/picom.conf ] && sed -i 's/^vsync = true;/vsync = false;/' ~/.config/picom/picom.conf

echo
echo ">> Pronto! Reinicie com: sudo reboot"
echo "   Depois: Alt+D -> steam / discord / zen   |   no terminal: spotify"
