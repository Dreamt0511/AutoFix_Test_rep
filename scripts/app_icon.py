#!/usr/bin/env python3
"""
Pocket Agent 应用图标生成脚本
将原始 logo 图片缩放为 Android 所需的各种分辨率图标。
"""
import os
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("需要安装 Pillow: pip install Pillow")
    sys.exit(1)

# ─── 配置 ─────────────────────────────────────────────────

SOURCE_IMAGE = r"D:\U 盘\pockent-logo.png"
OUTPUT_BASE = Path(__file__).parent

# Android mipmap 分辨率
MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive Icon 分辨率
ADAPTIVE_SIZES = {
    "mipmap-anydpi-v26": 108,  # 前景层
    "mipmap-anydpi-v26_adaptive_bg": 108,  # 背景层
}

# Google Play Store 图标
PLAY_STORE_ICON_SIZE = 512

def create_output_dirs(base_path: Path) -> None:
    """创建输出目录"""
    for dir_name in list(MIPMAP_SIZES.keys()) + ["play_store"]:
        (base_path / dir_name).mkdir(parents=True, exist_ok=True)

def generate_icons(source_path: str, output_base: Path) -> None:
    """生成所有分辨率的图标"""
    # 加载原图
    img = Image.open(source_path).convert("RGBA")
    print(f"原图尺寸: {img.size}")

    create_output_dirs(output_base)

    results = []

    # 生成 mipmap 图标
    for dir_name, size in MIPMAP_SIZES.items():
        output_path = output_base / dir_name / "ic_launcher.png"
        resized = img.resize((size, size), Image.LANCZOS)
        resized.save(output_path, "PNG")
        results.append((dir_name, size, str(output_path)))
        print(f"  ✓ {dir_name}: {size}x{size}")

    # 生成圆形图标
    for dir_name in MIPMAP_SIZES.keys():
        output_path = output_base / dir_name / "ic_launcher_round.png"
        size = MIPMAP_SIZES[dir_name]
        resized = img.resize((size, size), Image.LANCZOS)
        # 裁剪为圆形
        mask = Image.new("L", (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        round_img = Image.new("RGBA", (size, size))
        round_img.paste(resized, (0, 0), mask)
        round_img.save(output_path, "PNG")
        results.append((f"{dir_name}_round", size, str(output_path)))
        print(f"  ✓ {dir_name}_round: {size}x{size}")

    # 生成 adaptive icon 前景层
    adaptive_bg_path = output_base / "mipmap-anydpi-v26" / "ic_launcher_foreground.png"
    foreground = img.resize((108, 108), Image.LANCZOS)
    foreground.save(adaptive_bg_path, "PNG")
    results.append(("adaptive_foreground", 108, str(adaptive_bg_path)))
    print(f"  ✓ adaptive_foreground: 108x108")

    # 生成 Google Play Store 图标
    play_store_path = output_base / "play_store" / "ic_launcher-playstore.png"
    play_icon = img.resize((PLAY_STORE_ICON_SIZE, PLAY_STORE_ICON_SIZE), Image.LANCZOS)
    play_icon.save(play_store_path, "PNG")
    results.append(("play_store", PLAY_STORE_ICON_SIZE, str(play_store_path)))
    print(f"  ✓ play_store: {PLAY_STORE_ICON_SIZE}x{PLAY_STORE_ICON_SIZE}")

    # 生成 web 图标 (favicon)
    favicon_path = output_base / "favicon.png"
    favicon = img.resize((192, 192), Image.LANCZOS)
    favicon.save(favicon_path, "PNG")
    results.append(("favicon", 192, str(favicon_path)))
    print(f"  ✓ favicon: 192x192")

    print(f"\n总共生成 {len(results)} 个图标文件")
    print(f"输出目录: {output_base}")

    return results

if __name__ == "__main__":
    source = sys.argv[1] if len(sys.argv) > 1 else SOURCE_IMAGE

    if not os.path.exists(source):
        print(f"错误: 找不到源图片 {source}")
        print("用法: python app_icon.py [image_path]")
        sys.exit(1)

    generate_icons(source, OUTPUT_BASE / "res")