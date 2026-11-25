#!/usr/bin/env bash
# Instala Zen, Spotify, ncspot (Spotify no terminal), Discord e Prism Launcher via Flatpak.
# Uso: bash programas.sh
set -euo pipefail

echo ">> Instalando Flatpak e Flathub..."
sudo apt update
sudo apt install -y flatpak
sudo flatpak remote-add --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo

echo ">> Instalando os programas..."
sudo flatpak install -y flathub \
  app.zen_browser.zen \
  com.spotify.Client \
  io.github.hrkfdn.ncspot \
  com.discordapp.Discord \
  org.prismlauncher.PrismLauncher

# atalho "ncspot" no terminal
if ! grep -q 'alias ncspot=' ~/.bashrc; then
  echo "alias ncspot='flatpak run io.github.hrkfdn.ncspot'" >> ~/.bashrc
fi

echo
echo ">> Pronto! Saia e entre de novo no Sway para os apps aparecerem no menu (Alt+D)."
echo "   Spotify no terminal: ncspot"
