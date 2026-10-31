"""Google Play graphics: captioned screenshots, the 512 px icon and the feature graphics, into docs/google-play/graphics.

python tools/play.py <raw_dir> <preview_dir>

<raw_dir> holds full-screen phone captures (1080 px wide) named <name>.png (English) and <name>.ru.png (Russian),
taken from the demo build (`./gradlew installDebug -Pdemo`); the names are the first column of SCREENS.
<preview_dir> is what `./gradlew testDebugUnitTest -Ppreview=<preview_dir>` rendered: the feature graphic
takes its scene from scenes.png there.
"""
import random
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FONTS = ROOT / "app/src/main/res/font"
OUT = ROOT / "docs/google-play/graphics"

# The icon's own violets, and the app's gold and lilac.
DEEP, NIGHT = (42, 34, 72), (24, 19, 44)
INK, GOLD, LILAC = (246, 243, 255), (255, 232, 154), (200, 188, 255)
SPARKS = [(255, 232, 154), (155, 130, 255), (238, 243, 251)]

# Status bar and gesture bar of the capturing phone (Mi 9, 1080x2340), cut off the captures.
TOP, BOTTOM = 100, 55

# name, then (headline, subline) in English and Russian.
SCREENS = [
    ("tasks", ("Your habits power a hero", "Habits, dailies and to-dos in one quest log"),
     ("Привычки ведут героя", "Привычки, ежедневные дела и задачи")),
    ("quests", ("Every task hits the boss", "Twelve bosses that come back stronger"),
     ("Каждое дело бьёт босса", "Двенадцать боссов возвращаются сильнее")),
    ("stable", ("Hatch 88 pets", "Eggs, potions, mounts and friendship"),
     ("Выведи 88 питомцев", "Яйца, зелья, скакуны и дружба")),
    ("shop", ("Gear you can see", "Try it on before you buy"),
     ("Снаряжение видно на герое", "Примерь, прежде чем купить")),
    ("home", ("Furnish a cozy home", "A room, an attic and a garden"),
     ("Обставь уютный дом", "Комната, чердак и сад")),
    ("card", ("A card every morning", "Thirty twists on the day's rules"),
     ("Карта каждое утро", "Тридцать поворотов в правилах дня")),
    ("history", ("See how you're doing", "A heatmap, streaks and stats for every task"),
     ("Смотри, как идут дела", "Тепловая карта, серии и статистика задач")),
    ("hero", ("Built for years", "53 achievements, up to five years together"),
     ("Хватит на годы", "53 достижения, вплоть до пяти лет вместе")),
]

TAGLINE = {
    "en": ("Turn your days", "into a quest"),
    "ru": ("Преврати свои дни", "в приключение"),
}


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONTS / name), size)


def night(w: int, h: int, seed: int) -> Image.Image:
    """The icon's violet, darker towards the bottom, with square pixel sparks."""
    g = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(g)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(DEEP[i] * (1 - t) + NIGHT[i] * t) for i in range(3)))
    rnd = random.Random(seed)
    for _ in range(w * h // 12000):
        x, y, s = rnd.randint(0, w), rnd.randint(0, h), rnd.choice([4, 4, 6, 8])
        d.rectangle((x, y, x + s - 1, y + s - 1), fill=rnd.choice(SPARKS))
    return g.convert("RGBA")


def haze(g: Image.Image, box: tuple[int, int, int, int]) -> None:
    """A soft dark patch under text, so sparks or sky behind it don't break up the letters."""
    x0, y0, x1, y1 = box
    im = Image.new("RGBA", (x1 - x0 + 80, y1 - y0 + 80), (0, 0, 0, 0))
    ImageDraw.Draw(im).rounded_rectangle((40, 40, x1 - x0 + 40, y1 - y0 + 40), 24, fill=NIGHT + (190,))
    g.alpha_composite(im.filter(ImageFilter.GaussianBlur(16)), (x0 - 40, y0 - 40))


def fit(text: str, name: str, size: int, width: int) -> ImageFont.FreeTypeFont:
    f = font(name, size)
    while f.getlength(text) > width:
        size -= 2
        f = font(name, size)
    return f


def card(src: Path, width: int) -> Image.Image:
    im = Image.open(src).convert("RGB")
    w, h = im.size
    im = im.crop((0, TOP, w, h - BOTTOM))
    im = im.resize((width, round(im.height * width / w)), Image.LANCZOS)
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), 40, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def screenshot(src: Path, head: str, sub: str, seed: int) -> Image.Image:
    W, H, CARD_TOP = 1080, 1920, 360
    g = night(W, H, seed)
    hf = fit(head, "pixel_bold.ttf", 120, W - 120)
    sf = fit(sub, "inter_semibold.ttf", 40, W - 120)
    haze(g, (60, 70, W - 60, 300))
    d = ImageDraw.Draw(g)
    d.text((W / 2, 150), head, font=hf, fill=GOLD, anchor="mm")
    d.text((W / 2, 250), sub, font=sf, fill=LILAC, anchor="mm")
    c = card(src, 800)
    c = c.crop((0, 0, c.width, min(c.height, H - CARD_TOP + 40)))  # runs off the bottom edge, as if held up
    x, y = (W - c.width) // 2, CARD_TOP
    shadow = Image.new("RGBA", (c.width + 100, c.height + 100), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((50, 62, 50 + c.width, 62 + c.height), 40, fill=(0, 0, 0, 180))
    g.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(22)), (x - 50, y - 50))
    g.alpha_composite(c, (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 3, y - 3, x + c.width + 2, y + c.height + 2), 43, outline=(155, 130, 255), width=3)
    return g.convert("RGB")


def logo_pixels() -> list[tuple[int, int, str]]:
    """docs/logo.svg is a 24x24 grid of 1x1 rects; the first rect is the rounded ground."""
    svg = (ROOT / "docs/logo.svg").read_text(encoding="utf-8")
    return [(int(x), int(y), c) for x, y, c in re.findall(r'<rect x="(\d+)" y="(\d+)" width="1" height="1" fill="(#[0-9A-Fa-f]{6})"/>', svg)]


def icon() -> Image.Image:
    """The logo on a full square of its own violet, so Play's mask finds no corners. 21 px a pixel, centred."""
    g = Image.new("RGB", (512, 512), DEEP)
    d = ImageDraw.Draw(g)
    s, off = 21, (512 - 24 * 21) // 2
    for x, y, c in logo_pixels():
        d.rectangle((off + x * s, off + y * s, off + x * s + s - 1, off + y * s + s - 1), fill=c)
    return g


def scene(preview: Path) -> Image.Image:
    """The desert with the fire dragon from scenes.png (14 scenes of 720x288, drawn at 4x)."""
    sheet = Image.open(preview / "scenes.png").convert("RGB")
    h = sheet.height // 14
    return sheet.crop((0, 5 * h, sheet.width, 6 * h)).resize((180, 72), Image.NEAREST)


def feature(lang: str, sc: Image.Image) -> Image.Image:
    W, H = 1024, 500
    big = sc.resize((sc.width * 7, sc.height * 7), Image.NEAREST)  # 1260x504
    g = big.crop((150, 0, 150 + W, H)).convert("RGBA")  # the hero on the left, the dragon on the right
    d = ImageDraw.Draw(g)
    d.text((48, 18), "Dayquest", font=font("pixel_bold.ttf", 132), fill=GOLD, stroke_width=6, stroke_fill=NIGHT)
    sub = font("inter_semibold.ttf", 34)
    for i, line in enumerate(TAGLINE[lang]):
        d.text((54, 176 + i * 46), line, font=sub, fill=INK, stroke_width=4, stroke_fill=NIGHT)
    return g.convert("RGB")


def main() -> None:
    raw, preview = Path(sys.argv[1]), Path(sys.argv[2])
    OUT.mkdir(parents=True, exist_ok=True)
    icon().save(OUT / "icon-512.png", optimize=True)
    sc = scene(preview)
    for lang, suffix, idx in (("en", "", 1), ("ru", ".ru", 2)):
        feature(lang, sc).save(OUT / f"feature-graphic-{lang}.png", optimize=True)
        shots = OUT / f"screenshots-{lang}"
        shots.mkdir(exist_ok=True)
        for f in shots.glob("*.png"):
            f.unlink()
        n = 0
        for i, entry in enumerate(SCREENS, 1):
            src = raw / f"{entry[0]}{suffix}.png"
            if not src.exists():
                print(f"missing {src}")
                continue
            n += 1
            screenshot(src, *entry[idx], i).save(shots / f"{n}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
