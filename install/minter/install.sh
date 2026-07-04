#!/usr/bin/env sh
set -eu

REPO="orioneee/LMU-Assister"
VERSION="${LMU_MINTER_VERSION:-latest}"
PORT="${LMU_MINTER_PORT:-8787}"

need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

download_url() {
  artifact="$1"
  if [ "$VERSION" = "latest" ]; then
    printf 'https://github.com/%s/releases/latest/download/%s\n' "$REPO" "$artifact"
  else
    printf 'https://github.com/%s/releases/download/%s/%s\n' "$REPO" "$VERSION" "$artifact"
  fi
}

need curl

os="$(uname -s)"
arch="$(uname -m)"
archive_type=""

case "$os" in
  Darwin)
    case "$arch" in
      arm64|aarch64) platform="macos-arm64" ;;
      x86_64|amd64) platform="macos-x64" ;;
      *) echo "Unsupported macOS architecture: $arch" >&2; exit 1 ;;
    esac
    archive_type="zip"
    install_dir="${LMU_MINTER_HOME:-$HOME/Library/Application Support/LMU Assister/Minter}"
    log_dir="$HOME/Library/Logs/LMU Assister"
    ;;
  Linux)
    case "$arch" in
      x86_64|amd64) platform="linux-x64" ;;
      *) echo "Unsupported Linux architecture: $arch" >&2; exit 1 ;;
    esac
    archive_type="tar.gz"
    install_dir="${LMU_MINTER_HOME:-${XDG_DATA_HOME:-$HOME/.local/share}/lmu-assister/minter}"
    log_dir="${XDG_STATE_HOME:-$HOME/.local/state}/lmu-assister"
    ;;
  *)
    echo "Unsupported OS: $os" >&2
    exit 1
    ;;
esac

artifact="lmu-minter-$platform.$archive_type"
url="$(download_url "$artifact")"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/lmu-minter.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT INT HUP TERM

echo "Downloading $artifact"
curl -fL "$url" -o "$work_dir/$artifact"

mkdir -p "$work_dir/extract"
case "$archive_type" in
  zip)
    need unzip
    unzip -q "$work_dir/$artifact" -d "$work_dir/extract"
    ;;
  tar.gz)
    tar -xzf "$work_dir/$artifact" -C "$work_dir/extract"
    ;;
esac

source_dir="$work_dir/extract/lmu-minter-$platform"
if [ ! -d "$source_dir" ]; then
  echo "Archive layout is invalid: $source_dir not found" >&2
  exit 1
fi

echo "Installing to $install_dir"
mkdir -p "$(dirname "$install_dir")" "$log_dir"
case "$os" in
  Darwin)
    launchctl unload "$HOME/Library/LaunchAgents/com.lmu-assister.minter.plist" >/dev/null 2>&1 || true
    ;;
  Linux)
    if command -v systemctl >/dev/null 2>&1; then
      systemctl --user stop lmu-minter.service >/dev/null 2>&1 || true
    fi
    ;;
esac
rm -rf "$install_dir"
mkdir -p "$install_dir"
cp -R "$source_dir/." "$install_dir/"
chmod +x "$install_dir/bin/lmu-minter"

install_macos_autostart() {
  plist_dir="$HOME/Library/LaunchAgents"
  plist="$plist_dir/com.lmu-assister.minter.plist"
  mkdir -p "$plist_dir"
  cat > "$plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>com.lmu-assister.minter</string>
  <key>ProgramArguments</key>
  <array>
    <string>$install_dir/bin/lmu-minter</string>
    <string>$PORT</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$install_dir</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$log_dir/minter.log</string>
  <key>StandardErrorPath</key>
  <string>$log_dir/minter.err.log</string>
</dict>
</plist>
EOF
  launchctl unload "$plist" >/dev/null 2>&1 || true
  launchctl load -w "$plist"
}

install_linux_autostart() {
  service_dir="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user"
  service="$service_dir/lmu-minter.service"
  mkdir -p "$service_dir"
  cat > "$service" <<EOF
[Unit]
Description=LMU Assister Minter
After=network-online.target

[Service]
Type=simple
WorkingDirectory=$install_dir
ExecStart=$install_dir/bin/lmu-minter $PORT
Restart=always
RestartSec=2

[Install]
WantedBy=default.target
EOF

  if command -v systemctl >/dev/null 2>&1 && systemctl --user daemon-reload >/dev/null 2>&1; then
    systemctl --user enable lmu-minter.service
    systemctl --user restart lmu-minter.service
  else
    autostart_dir="${XDG_CONFIG_HOME:-$HOME/.config}/autostart"
    mkdir -p "$autostart_dir"
    cat > "$autostart_dir/lmu-minter.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=LMU Assister Minter
Exec=$install_dir/bin/lmu-minter $PORT
X-GNOME-Autostart-enabled=true
EOF
    nohup "$install_dir/bin/lmu-minter" "$PORT" > "$log_dir/minter.log" 2>&1 &
  fi
}

case "$os" in
  Darwin) install_macos_autostart ;;
  Linux) install_linux_autostart ;;
esac

health_url="http://127.0.0.1:$PORT/health"
for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  if curl -fsS "$health_url" >/dev/null 2>&1; then
    echo "LMU Minter is installed and running: $health_url"
    exit 0
  fi
  sleep 1
done

echo "LMU Minter is installed, but health check did not answer yet: $health_url"
