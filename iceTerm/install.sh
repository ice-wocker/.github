#!/usr/bin/env bash
#
# iceTerm installer
# Supports: Android (Termux), Debian/Ubuntu, Arch, Alpine, macOS
#

set -e

GREEN="\033[32m"
CYAN="\033[36m"
YELLOW="\033[33m"
RESET="\033[0m"

echo -e "${CYAN}[iceTerm] Installing iceTerm workstation...${RESET}"

# 1. Verify Python 3
if ! command -v python3 >/dev/null 2>&1; then
    echo -e "${YELLOW}Python 3 not found. Attempting automatic installation...${RESET}"
    if [ -n "$PREFIX" ] && [ -d "$PREFIX" ]; then
        # Termux
        pkg update -y && pkg install -y python
    elif command -v apt-get >/dev/null 2>&1; then
        sudo apt-get update && sudo apt-get install -y python3
    elif command -v pacman >/dev/null 2>&1; then
        sudo pacman -Sy --noconfirm python
    elif command -v apk >/dev/null 2>&1; then
        apk add --no-cache python3
    else
        echo "Error: Python 3 must be installed manually."
        exit 1
    fi
fi

# 2. Determine install path
INSTALL_DIR=""
if [ -n "$PREFIX" ] && [ -d "$PREFIX/bin" ]; then
    INSTALL_DIR="$PREFIX/bin"
elif [ -w "/usr/local/bin" ]; then
    INSTALL_DIR="/usr/local/bin"
elif [ -d "$HOME/.local/bin" ]; then
    INSTALL_DIR="$HOME/.local/bin"
else
    mkdir -p "$HOME/.local/bin"
    INSTALL_DIR="$HOME/.local/bin"
fi

TARGET="$INSTALL_DIR/iceterm"

# 3. Copy or download
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
if [ -f "$SCRIPT_DIR/ice_term.py" ]; then
    cp "$SCRIPT_DIR/ice_term.py" "$TARGET"
else
    curl -fsSL https://raw.githubusercontent.com/ice-wocker/iceTerm/main/ice_term.py -o "$TARGET"
fi

chmod +x "$TARGET"

echo -e "${GREEN}[OK] iceTerm installed to: $TARGET${RESET}"
echo -e "Run ${CYAN}iceterm${RESET} to launch."
