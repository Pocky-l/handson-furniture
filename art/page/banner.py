"""Title banner of the Hands-On Furniture page: the full fridge at night, its opening straightened, on the right; the name in
Fraunces (OFL, fonts/) on the left over a blurred, darkened copy of the same shot.

Usage (from art/page): python banner.py <square fridge image, e.g. the logo before its cut> <out.png>
Writes a 1600x520 PNG.
"""
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1600, 520
SIDE = 560  # the fridge's square, a little taller than the banner: its top and bottom frame are cut


def background(square):
    wide = square.resize((W, W), Image.LANCZOS).crop((0, (W - H) // 2, W, (W + H) // 2))
    wide = np.array(wide.filter(ImageFilter.GaussianBlur(18)), np.float32)
    x = np.linspace(0, 1, W)[None, :, None]
    shade = 0.22 + 0.25 * x
    night = np.array([14, 14, 30], np.float32)
    return Image.fromarray(np.clip(wide * shade + night * (1 - shade), 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def fridge(square):
    image = square.resize((SIDE, SIDE), Image.LANCZOS).convert("RGBA")
    x = np.linspace(0, 1, SIDE)[None, :]
    alpha = np.clip(x / 0.18, 0, 1) * np.ones((SIDE, 1))
    image.putalpha(Image.fromarray((alpha * 255).astype(np.uint8), "L"))
    return image


def title_layer():
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    font = ImageFont.truetype("fonts/Fraunces.ttf", 104)
    font.set_variation_by_axes([144, 700, 100, 1])
    small = ImageFont.truetype("fonts/Fraunces.ttf", 29)
    small.set_variation_by_axes([24, 500, 50, 0])
    tx, ty = 70, 140
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).text((tx, ty), "Hands-On Furniture", font=font, fill=255)
    glow = Image.new("RGBA", (W, H), (255, 190, 110, 0))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(22)).point(lambda a: int(a * 0.4)))
    layer.alpha_composite(glow)
    shadow = Image.new("RGBA", (W, H), (6, 6, 16, 0))
    shadow.putalpha(mask.filter(ImageFilter.GaussianBlur(3)).point(lambda a: int(a * 0.85)))
    layer.alpha_composite(shadow, (4, 6))
    grad = np.zeros((H, W, 4), np.uint8)
    gy = np.clip((np.arange(H)[:, None] - ty - 25) / 140.0, 0, 1)
    top_c, bot_c = np.array([255, 244, 214]), np.array([240, 150, 70])
    grad[..., :3] = (top_c * (1 - gy[..., None]) + bot_c * gy[..., None]).astype(np.uint8)
    grad[..., 3] = np.array(mask)
    layer.alpha_composite(Image.fromarray(grad, "RGBA"))
    d = ImageDraw.Draw(layer)
    text = "FRIDGE  ·  FREEZER  ·  CABINETS  ·  DRAWERS"
    line_y = ty + 160
    d.line([(tx + 6, line_y), (tx + 6 + d.textlength(text, font=small), line_y)], fill=(236, 150, 74, 210), width=2)
    d.text((tx + 6, line_y + 20), text, font=small, fill=(255, 232, 190, 255))
    d.text((tx + 6, line_y + 64), "An addon for MrCrayfish's Furniture Mod: Refurbished", font=small,
           fill=(200, 196, 220, 220))
    return layer


if __name__ == "__main__":
    square = Image.open(sys.argv[1]).convert("RGB")
    banner = background(square)
    banner.alpha_composite(fridge(square), (W - SIDE - 30, (H - SIDE) // 2))
    banner.alpha_composite(title_layer())
    banner.convert("RGB").save(sys.argv[2], optimize=True)
    print(sys.argv[2])
