from PIL import Image, ImageDraw
import os

BASE = r"D:\New folder (2)\New folder (7)\streamflix-1.7.222\app\src\main\res"
LOGO_PATH = r"D:\New folder (2)\New folder (7)\images.png"
BG_COLOR = (0, 0, 0)

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Banner sizes (for Android TV launcher banner)
BANNER_DENSITIES = {
    "mdpi": 320,
    "hdpi": 480,
    "xhdpi": 640,
    "xxhdpi": 960,
    "xxxhdpi": 1280,
}

def extract():
    img = Image.open(LOGO_PATH).convert("RGBA")
    w, h = img.size
    data = list(img.getdata())
    new_data = [(0, 0, 0, 0) if p[0] > 250 and p[1] > 250 and p[2] > 250 else (p[0], p[1], p[2], 255) for p in data]
    out = Image.new("RGBA", (w, h))
    out.putdata(new_data)
    return out

def generate_all():
    logo = extract()
    bbox = logo.getbbox()
    if bbox:
        logo = logo.crop(bbox)
    lw, lh = logo.size

    # Generate launcher icons
    for density, dp_size in DENSITIES.items():
        mipmap_dir = os.path.join(BASE, f"mipmap-{density}")
        os.makedirs(mipmap_dir, exist_ok=True)

        target = int(dp_size * 0.70)
        scale = min(target / lw, target / lh)
        new_w = max(int(lw * scale), 1)
        new_h = max(int(lh * scale), 1)
        resized = logo.resize((new_w, new_h), Image.LANCZOS)

        fg = Image.new("RGBA", (dp_size, dp_size), (0, 0, 0, 0))
        ox = (dp_size - new_w) // 2
        oy = (dp_size - new_h) // 2
        fg.paste(resized, (ox, oy), resized)
        fg.save(os.path.join(mipmap_dir, "ic_launcher_foreground.png"))

        bg = Image.new("RGBA", (dp_size, dp_size), BG_COLOR + (255,))
        Image.alpha_composite(bg, fg).convert("RGB").save(
            os.path.join(mipmap_dir, "ic_launcher.png"))

        mask = Image.new("L", (dp_size, dp_size), 0)
        ImageDraw.Draw(mask).ellipse([(0, 0), (dp_size, dp_size)], fill=255)
        round_icon = Image.new("RGBA", (dp_size, dp_size), (0, 0, 0, 0))
        round_icon.paste(Image.alpha_composite(bg, fg), (0, 0), mask)
        round_icon.convert("RGB").save(os.path.join(mipmap_dir, "ic_launcher_round.png"))

        print(f"  icon {density} ({dp_size}x{dp_size}) OK")

    # Generate banner foreground (foreground-only, for adaptive banner)
    banner_size = 320
    target = int(banner_size * 0.50)
    scale = min(target / lw, target / lh)
    new_w = max(int(lw * scale), 1)
    new_h = max(int(lh * scale), 1)
    resized = logo.resize((new_w, new_h), Image.LANCZOS)
    banner_fg = Image.new("RGBA", (banner_size, banner_size), (0, 0, 0, 0))
    ox = (banner_size - new_w) // 2
    oy = (banner_size - new_h) // 2
    banner_fg.paste(resized, (ox, oy), resized)
    banner_fg.save(os.path.join(BASE, "mipmap-xhdpi", "ic_banner_foreground.png"), "PNG")
    print(f"  banner foreground ({banner_size}x{banner_size}) OK")

    # Generate full banner for TV launcher (with background)
    for density, bp_size in BANNER_DENSITIES.items():
        mipmap_dir = os.path.join(BASE, f"mipmap-{density}")
        os.makedirs(mipmap_dir, exist_ok=True)

        banner_w = bp_size * 2  # banners are wider (16:9 ratio)
        banner_h = bp_size

        logo_target = int(banner_h * 0.50)
        scale = min(logo_target / lw, logo_target / lh)
        new_w = max(int(lw * scale), 1)
        new_h = max(int(lh * scale), 1)
        resized = logo.resize((new_w, new_h), Image.LANCZOS)

        banner = Image.new("RGBA", (banner_w, banner_h), BG_COLOR + (255,))
        ox = (banner_w - new_w) // 2
        oy = (banner_h - new_h) // 2
        banner.paste(resized, (ox, oy), resized)
        banner.convert("RGB").save(os.path.join(mipmap_dir, "ic_banner.png"))
        print(f"  banner {density} ({banner_w}x{banner_h}) OK")

    # Generate small 24dp icon for nav header (as drawable PNG)
    nav_size = 72  # ldpi baseline for 24dp
    target = int(nav_size * 0.80)
    scale = min(target / lw, target / lh)
    new_w = max(int(lw * scale), 1)
    new_h = max(int(lh * scale), 1)
    resized = logo.resize((new_w, new_h), Image.LANCZOS)
    nav_fg = Image.new("RGBA", (nav_size, nav_size), (0, 0, 0, 0))
    ox = (nav_size - new_w) // 2
    oy = (nav_size - new_h) // 2
    nav_fg.paste(resized, (ox, oy), resized)

    drawable_dir = os.path.join(BASE, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)
    nav_fg.save(os.path.join(drawable_dir, "ic_nifflex_logo.png"), "PNG")
    print(f"  nav logo ({nav_size}x{nav_size}) OK")

def generate_playstore():
    size = 512
    out = os.path.join(BASE, "..", "ic_launcher-playstore.png")
    logo = extract()
    bbox = logo.getbbox()
    if bbox:
        logo = logo.crop(bbox)
    lw, lh = logo.size
    target = int(size * 0.70)
    scale = min(target / lw, target / lh)
    new_w = max(int(lw * scale), 1)
    new_h = max(int(lh * scale), 1)
    resized = logo.resize((new_w, new_h), Image.LANCZOS)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox = (size - new_w) // 2
    oy = (size - new_h) // 2
    fg.paste(resized, (ox, oy), resized)
    bg = Image.new("RGBA", (size, size), BG_COLOR + (255,))
    Image.alpha_composite(bg, fg).convert("RGB").save(out)
    print(f"  playstore ({size}x{size}) OK")

if __name__ == "__main__":
    print("Generating icons...")
    generate_all()
    generate_playstore()
    print("Done!")
