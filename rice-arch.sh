#!/usr/bin/env bash
# Rice "clean/claro" para Arch Linux + Sway (instalado pelo archinstall)
# Waybar + Alacritty + Rofi + Fastfetch, Alt como tecla principal, teclado US
# Uso: bash rice-arch.sh   (NÃO rode como root; o script usa sudo quando precisa)
set -euo pipefail

echo ">> Instalando pacotes..."
sudo pacman -Syu --needed --noconfirm \
  sway swaybg swayidle swaylock waybar alacritty rofi fastfetch \
  grim slurp wl-clipboard xorg-xwayland pavucontrol \
  xdg-desktop-portal-wlr ttf-font-awesome unzip curl git

echo ">> Instalando fonte Victor Mono (itálico cursivo do print)..."
if ! fc-list | grep -qi "Victor Mono"; then
  mkdir -p ~/.local/share/fonts/VictorMono /tmp/victor
  if curl -fL -o /tmp/victor/VictorMonoAll.zip https://rubjo.github.io/victor-mono/VictorMonoAll.zip; then
    unzip -oq /tmp/victor/VictorMonoAll.zip -d /tmp/victor
    find /tmp/victor -name '*.ttf' -path '*TTF*' -exec cp {} ~/.local/share/fonts/VictorMono/ \;
    fc-cache -f
  else
    echo "!! Não consegui baixar a fonte. Baixe em https://rubjo.github.io/victor-mono e coloque os .ttf em ~/.local/share/fonts"
  fi
fi

mkdir -p ~/.config/{sway,waybar,alacritty,fastfetch,rofi}

# ---------------- SWAY ----------------
cat > ~/.config/sway/config <<'EOF'
set $mod Mod1
set $term alacritty
set $menu rofi -show drun

font pango:Victor Mono Italic 10

# Wallpaper: coloque sua imagem em ~/.config/sway/wallpaper.jpg
output * bg #f0f0f0 solid_color
exec_always [ -f ~/.config/sway/wallpaper.jpg ] && swaymsg output '*' bg ~/.config/sway/wallpaper.jpg fill

# Monitor: FreeSync e 144 Hz
# Veja o nome e os modos com: swaymsg -t get_outputs
output * adaptive_sync on
# output DP-1 mode 1920x1080@143.981Hz

# Teclado US e mouse sem aceleração (1:1)
input type:keyboard {
    xkb_layout us
}
input type:pointer {
    accel_profile flat
    pointer_accel 0
}

# Visual
default_border pixel 2
default_floating_border pixel 2
gaps inner 12
gaps outer 6

#                       borda    fundo    texto    indicador borda-filho
client.focused          #d4a0a8  #d4a0a8  #ffffff  #d4a0a8   #d4a0a8
client.focused_inactive #b8b8b8  #ffffff  #555555  #b8b8b8   #b8b8b8
client.unfocused        #d0d0d0  #ffffff  #888888  #d0d0d0   #d0d0d0

# Terminal flutuando centralizado
for_window [app_id="Alacritty"] floating enable, resize set 920 640, move position center

# Jogos: menos input lag em tela cheia
for_window [class="steam_app_.*"] allow_tearing yes
for_window [app_id="gamescope"] allow_tearing yes

# Barra
bar {
    swaybar_command waybar
}

# Atalhos
bindsym $mod+Return exec $term
bindsym $mod+d exec $menu
bindsym $mod+q kill
bindsym $mod+Shift+r reload
bindsym $mod+Shift+e exec swaynag -t warning -m 'Sair do Sway?' -B 'Sim' 'swaymsg exit'
bindsym $mod+f fullscreen toggle
bindsym $mod+space floating toggle
bindsym Print exec grim -g "$(slurp)" - | wl-copy

bindsym $mod+h focus left
bindsym $mod+j focus down
bindsym $mod+k focus up
bindsym $mod+l focus right
bindsym $mod+Shift+h move left
bindsym $mod+Shift+j move down
bindsym $mod+Shift+k move up
bindsym $mod+Shift+l move right

bindsym $mod+1 workspace number 1
bindsym $mod+2 workspace number 2
bindsym $mod+3 workspace number 3
bindsym $mod+4 workspace number 4
bindsym $mod+5 workspace number 5
bindsym $mod+Shift+1 move container to workspace number 1
bindsym $mod+Shift+2 move container to workspace number 2
bindsym $mod+Shift+3 move container to workspace number 3
bindsym $mod+Shift+4 move container to workspace number 4
bindsym $mod+Shift+5 move container to workspace number 5

floating_modifier $mod normal

include /etc/sway/config.d/*
EOF

# ---------------- WAYBAR (flutuante, quadrada, simétrica) ----------------
cat > ~/.config/waybar/config.jsonc <<'EOF'
{
  "layer": "top",
  "position": "top",
  "height": 26,
  "margin-top": 12,
  "margin-left": 20,
  "margin-right": 20,
  "modules-left": ["sway/workspaces"],
  "modules-center": ["clock"],
  "modules-right": ["cpu", "memory", "network", "pulseaudio"],

  "sway/workspaces": { "format": "{name}" },
  "clock": {
    "format": "{:%a, %d %b %H:%M}",
    "locale": "en_US.UTF-8"
  },
  "cpu": { "format": "● {usage}%", "interval": 3 },
  "memory": { "format": "■ {percentage}%", "interval": 5 },
  "network": {
    "format-ethernet": "◘ {ipaddr}",
    "format-wifi": "◘ {ipaddr}",
    "format-disconnected": "◘ offline"
  },
  "pulseaudio": {
    "format": "• {volume}%",
    "format-muted": "• mudo",
    "on-click": "pavucontrol"
  }
}
EOF

cat > ~/.config/waybar/style.css <<'EOF'
* {
  font-family: "Victor Mono";
  font-style: italic;
  font-size: 11px;
  min-height: 0;
  border: none;
  border-radius: 0;
}

window#waybar {
  background: rgba(255, 255, 255, 0.92);
  color: #6a6a6a;
  border: 1px solid #c8c8c8;
  border-radius: 0;
}

#workspaces button {
  color: #999999;
  padding: 0 8px;
  background: transparent;
  border-bottom: 2px solid transparent;
}
#workspaces button.focused {
  color: #333333;
  border-bottom: 2px solid #333333;
}

#clock { color: #666666; }

#cpu, #memory, #network, #pulseaudio {
  padding: 0 8px;
  color: #777777;
}
EOF

# ---------------- ALACRITTY ----------------
cat > ~/.config/alacritty/alacritty.toml <<'EOF'
[window]
padding = { x = 18, y = 14 }

[font]
size = 11.0
normal = { family = "Victor Mono", style = "Italic" }
bold = { family = "Victor Mono", style = "Bold Italic" }
italic = { family = "Victor Mono", style = "Italic" }

[colors.primary]
background = "#ffffff"
foreground = "#5a5a5a"

[colors.cursor]
text = "#ffffff"
cursor = "#3a3a3a"

[colors.normal]
black = "#3a3a3a"
red = "#8c2a3a"
green = "#5f7f5f"
yellow = "#9a7b3c"
blue = "#4f6a8f"
magenta = "#7d5a8c"
cyan = "#4f8a8a"
white = "#bdbdbd"

[colors.bright]
black = "#707070"
red = "#a8394a"
green = "#6f9a6f"
yellow = "#b38f45"
blue = "#5f7fa8"
magenta = "#9468a6"
cyan = "#5fa0a0"
white = "#e0e0e0"
EOF

# ---------------- ROFI (mesmo estilo do terminal) ----------------
cat > ~/.config/rofi/config.rasi <<'EOF'
configuration {
  modi: "drun,run";
  show-icons: false;
  display-drun: "›";
  display-run: "›";
  drun-display-format: "{name}";
  font: "Victor Mono Italic 11";
}

* {
  bg: #ffffff;
  fg: #5a5a5a;
  dim: #9a9a9a;
  accent: #d4a0a8;
  sel: #f6eaec;
  background-color: transparent;
  text-color: @fg;
}

window {
  width: 600px;
  background-color: @bg;
  border: 2px;
  border-color: @accent;
  border-radius: 0;
  padding: 18px;
}

mainbox { children: [ inputbar, listview ]; spacing: 12px; }
inputbar { children: [ prompt, entry ]; spacing: 8px; }
prompt { text-color: @fg; }
entry { placeholder: ""; cursor: text; }

listview {
  lines: 8;
  columns: 1;
  fixed-height: false;
  scrollbar: false;
  spacing: 2px;
}

element { padding: 4px 6px; }
element normal.normal, element alternate.normal { background-color: transparent; text-color: @dim; }
element selected.normal { background-color: @sel; text-color: @fg; }
element-text { background-color: inherit; text-color: inherit; }
EOF

# ---------------- FASTFETCH ----------------
cat > ~/.config/fastfetch/config.jsonc <<'EOF'
{
  "$schema": "https://github.com/fastfetch-cli/fastfetch/raw/dev/doc/json_schema.json",
  "logo": {
    "type": "data",
    "source": " /\\_/\\ \n( o.o )\n > ^ < ",
    "padding": { "top": 0, "left": 1, "right": 3 },
    "color": { "1": "black" }
  },
  "display": {
    "separator": "  ",
    "color": { "keys": "black" }
  },
  "modules": [
    { "type": "os",     "key": "OS    " },
    { "type": "cpu",    "key": "CPU   " },
    { "type": "gpu",    "key": "GPU   " },
    { "type": "memory", "key": "Memory" }
  ]
}
EOF

# ---------------- BASH: prompt minimalista + fastfetch ----------------
if ! grep -q '# rice-prompt' ~/.bashrc; then
cat >> ~/.bashrc <<'EOF'

# rice-prompt
PS1='\n\[\e[90m\]~\n\[\e[30m\]› \[\e[0m\]'
[[ $- == *i* ]] && command -v fastfetch >/dev/null && fastfetch
EOF
fi

echo
echo ">> Pronto!"
echo "   1) Copie seu wallpaper para ~/.config/sway/wallpaper.jpg"
echo "   2) Saia e entre de novo no Sway (ou Alt+Shift+R para recarregar)"
echo "   3) Alt+Enter abre o terminal, Alt+D abre o menu"
echo "   4) Para 144 Hz: rode 'swaymsg -t get_outputs' e ajuste a linha 'output' em ~/.config/sway/config"
