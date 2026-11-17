"""Title banner of the Hands-On Furniture page, laid out as Hands-On Delight's and Vanilla's: a kitchen of Refurbished
furniture shot in game (out of focus and darkened behind the title, the open fridge sharp on the right), the name in
Fraunces (OFL, fonts/). Shot: Shots `hf_banner.txt` of Diegesis (window 1600x900).

Usage (from art/page): python banner.py <shot.png> <out.png> [top 0..1]
Writes a 1600x520 PNG.
"""
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1600, 520


def scene(src, top):
    src = src.resize((W, round(src.height * W / src.width)), Image.LANCZOS)
    y0 = max(0, min(src.height - H, int(src.height * top)))
    crop = src.crop((0, y0, W, y0 + H))
    sharp = np.array(crop, np.float32)
    soft = np.array(crop.filter(ImageFilter.GaussianBlur(7)), np.float32)
    x = np.linspace(0, 1, W)[None, :, None]
    y = np.linspace(0, 1, H)[:, None, None]
    focus = np.clip((x - 0.45) / 0.3, 0, 1)
    shade = np.clip(1.0 - 0.8 * np.clip((0.78 - x) / 0.6, 0, 1) ** 1.1, 0.2, 1.0)
    shade = shade * (1.0 - 0.3 * np.clip((y - 0.75) / 0.25, 0, 1))
    bg = soft * (1 - focus) + sharp * focus
    tint = np.array([34, 18, 10], np.float32)  # warm kitchen brown, not black
    bg = bg * shade + tint * (1 - shade)
    return Image.fromarray(np.clip(bg, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def title_layer():
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    font = ImageFont.truetype("fonts/Fraunces.ttf", 104)
    font.set_variation_by_axes([144, 700, 100, 1])
    small = ImageFont.truetype("fonts/Fraunces.ttf", 29)
    small.set_variation_by_axes([24, 500, 50, 0])
    tx, ty = 70, 140
    title = "Hands-On Furniture"
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).text((tx, ty), title, font=font, fill=255)
    glow = Image.new("RGBA", (W, H), (255, 160, 70, 0))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(22)).point(lambda a: int(a * 0.45)))
    layer.alpha_composite(glow)
    shadow = Image.new("RGBA", (W, H), (20, 8, 0, 0))
    shadow.putalpha(mask.filter(ImageFilter.GaussianBlur(3)).point(lambda a: int(a * 0.85)))
    layer.alpha_composite(shadow, (4, 6))
    grad = np.zeros((H, W, 4), np.uint8)
    gy = np.clip((np.arange(H)[:, None] - ty - 25) / 140.0, 0, 1)
    top_c, bot_c = np.array([255, 238, 196]), np.array([228, 120, 52])
    grad[..., :3] = (top_c * (1 - gy[..., None]) + bot_c * gy[..., None]).astype(np.uint8)
    grad[..., 3] = np.array(mask)
    layer.alpha_composite(Image.fromarray(grad, "RGBA"))
    d = ImageDraw.Draw(layer)
    text = "An addon for MrCrayfish's Furniture Mod: Refurbished"  # no list of stations: it ages with every new one
    line_y = ty + 160
    end = tx + font.getbbox(title)[2]  # the rule runs to the title's last letter
    d.line([(tx + 6, line_y), (end, line_y)], fill=(226, 140, 66, 210), width=2)
    d.text((tx + 6, line_y + 20), text, font=small, fill=(255, 226, 180, 255))
    return layer


if __name__ == "__main__":
    shot, out = sys.argv[1], sys.argv[2]
    top = float(sys.argv[3]) if len(sys.argv) > 3 else 0.3
    banner = scene(Image.open(shot).convert("RGB"), top)
    banner.alpha_composite(title_layer())
    banner.convert("RGB").save(out, optimize=True)
    print(out)
