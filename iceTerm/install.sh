#!/usr/bin/env bash
#
# 🧊 iceTerm (冰端) 一键安装脚本
# 支持: Android (Termux), Debian/Ubuntu, Arch, Alpine, macOS
#

set -e

GREEN="\033[32m"
CYAN="\033[36m"
YELLOW="\033[33m"
RESET="\033[0m"

echo -e "${CYAN}🧊 正在安装 iceTerm (冰端)...${RESET}"

# 1. 检查 Python 3 环境
if ! command -v python3 >/dev/null 2>&1; then
    echo -e "${YELLOW}检测到未安装 Python 3，正在尝试自动安装...${RESET}"
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
        echo "请先手动安装 Python 3！"
        exit 1
    fi
fi

# 2. 确定安装路径
INSTALL_DIR=""
if [ -n "$PREFIX" ] && [ -d "$PREFIX/bin" ]; then
    # Termux 环境
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

# 3. 复制或下载
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
if [ -f "$SCRIPT_DIR/ice_term.py" ]; then
    cp "$SCRIPT_DIR/ice_term.py" "$TARGET"
else
    curl -fsSL https://raw.githubusercontent.com/ice-wocker/iceTerm/main/ice_term.py -o "$TARGET"
fi

chmod +x "$TARGET"

echo -e "${GREEN}✓ iceTerm 安装成功！路径: $TARGET${RESET}"
echo -e "随时输入 ${CYAN}iceterm${RESET} 即可启动！"
