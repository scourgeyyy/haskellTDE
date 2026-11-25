#!/usr/bin/env bash
# Rice estilo "clean/claro" para Debian 13 (Trixie) com i3 + Polybar + Alacritty + Fastfetch
# Uso: bash rice-debian-i3.sh   (NÃO rode como root; o script usa sudo quando precisa)
set -euo pipefail

echo ">> Instalando pacotes..."
sudo apt update
sudo apt install -y xorg xserver-xorg xauth i3 polybar alacritty fastfetch feh picom rofi \
  maim xclip pulseaudio-utils pavucontrol unzip curl git python3 fonts-font-awesome \
  build-essential libpam0g-dev libxcb-xkb-dev brightnessctl

echo ">> Instalando fonte Victor Mono (itálico cursivo do print)..."
mkdir -p ~/.local/share/fonts/VictorMono /tmp/victor
if curl -fL -o /tmp/victor/VictorMonoAll.zip https://rubjo.github.io/victor-mono/VictorMonoAll.zip; then
  unzip -oq /tmp/victor/VictorMonoAll.zip -d /tmp/victor
  find /tmp/victor -name '*.ttf' -path '*TTF*' -exec cp {} ~/.local/share/fonts/VictorMono/ \;
  fc-cache -f
else
  echo "!! Não consegui baixar a fonte. Baixe em https://rubjo.github.io/victor-mono e coloque os .ttf em ~/.local/share/fonts"
fi

mkdir -p ~/.config/{i3,polybar,alacritty,fastfetch,picom,rofi}

# ---------------- I3 ----------------
cat > ~/.config/i3/config <<'EOF'
set $mod Mod1
set $term alacritty

font pango:Victor Mono Italic 10

# Wallpaper: coloque sua imagem em ~/.config/i3/wallpaper.jpg
exec_always --no-startup-id feh --bg-fill ~/.config/i3/wallpaper.jpg
exec_always --no-startup-id ~/.config/polybar/launch.sh
exec --no-startup-id picom -b

# Visual
default_border pixel 2
default_floating_border pixel 2
gaps inner 12
gaps outer 6
gaps top 48

#                       borda    fundo    texto    indicador borda-filho
client.focused          #d4a0a8  #d4a0a8  #ffffff  #d4a0a8   #d4a0a8
client.focused_inactive #b8b8b8  #ffffff  #555555  #b8b8b8   #b8b8b8
client.unfocused        #d0d0d0  #ffffff  #888888  #d0d0d0   #d0d0d0

# Terminal flutuando centralizado, como no print
for_window [class="Alacritty"] floating enable, resize set 920 640, move position center

# Atalhos
bindsym $mod+Return exec $term
bindsym $mod+d exec rofi -show drun
bindsym $mod+q kill
bindsym $mod+Shift+r restart
bindsym $mod+Shift+e exec i3-nagbar -t warning -m 'Sair do i3?' -B 'Sim' 'i3-msg exit'
bindsym $mod+f fullscreen toggle
bindsym $mod+space floating toggle
bindsym Print exec --no-startup-id maim -s | xclip -selection clipboard -t image/png

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

floating_modifier $mod

# Teclado US
exec --no-startup-id setxkbmap us
EOF

# ---------------- PICOM (cantos arredondados/sombra leve) ----------------
cat > ~/.config/picom/picom.conf <<'EOF'
backend = "glx";
vsync = true;
corner-radius = 2;
shadow = true;
shadow-radius = 10;
shadow-opacity = 0.15;
rounded-corners-exclude = [ "class_g = 'Polybar'" ];
EOF

# ---------------- POLYBAR ----------------
cat > ~/.config/polybar/config.ini <<'EOF'
[colors]
bg = #ebffffff
fg = #6a6a6a
dim = #999999
active = #333333
border = #c8c8c8

[bar/main]
width = 100%:-40
offset-x = 20
offset-y = 12
height = 26
radius = 0
background = ${colors.bg}
foreground = ${colors.fg}
border-size = 1
border-color = ${colors.border}
padding-left = 1
padding-right = 2
module-margin = 1
font-0 = "Victor Mono:style=Italic:size=9;2"
modules-left = i3
modules-center = date
modules-right = cpu memory network volume
override-redirect = true
wm-restack = i3

[module/i3]
type = internal/i3
pin-workspaces = true
format = <label-state>
label-focused = %name%
label-focused-foreground = ${colors.active}
label-focused-underline = ${colors.active}
label-focused-padding = 1
label-unfocused = %name%
label-unfocused-foreground = ${colors.dim}
label-unfocused-padding = 1
label-visible = %name%
label-visible-padding = 1
label-urgent = %name%
label-urgent-foreground = #8c2a3a
label-urgent-padding = 1

[module/date]
type = internal/date
interval = 1
date = %a, %d %b
time = %H:%M
label = %date% %time%

[module/cpu]
type = internal/cpu
interval = 3
label = ● %percentage%%

[module/memory]
type = internal/memory
interval = 5
label = ■ %percentage_used%%

[module/network]
type = internal/network
interface-type = wired
interval = 5
label-connected = ◘ %local_ip%
label-disconnected = ◘ offline

[module/volume]
type = internal/pulseaudio
format-volume = <label-volume>
label-volume = • %percentage%%
label-muted = • mudo
click-right = pavucontrol

[settings]
screenchange-reload = true
EOF

cat > ~/.config/polybar/launch.sh <<'EOF'
#!/usr/bin/env bash
killall -q polybar
while pgrep -u $UID -x polybar >/dev/null; do sleep 0.2; done
polybar main 2>&1 | tee -a /tmp/polybar.log & disown
EOF
chmod +x ~/.config/polybar/launch.sh

# ---------------- ALACRITTY (terminal) ----------------
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

# ---------------- ROFI (menu drun, mesmo estilo do terminal) ----------------
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
# baixa só do servidor oficial do Zig
curl -fL "$ZIG_URL" -o "$LY_DIR/zig.tar.xz"
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
echo ">> Pronto!"
echo "   1) Copie seu wallpaper para ~/.config/i3/wallpaper.jpg"
echo "   2) Reinicie (sudo reboot) e escolha 'i3' na tela do Ly (setas laterais)"
echo "   3) Alt+Enter abre o terminal, Alt+D abre o menu"
