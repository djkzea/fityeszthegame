"""
The installers' own artwork, drawn from the game's palette and type.

  python3 packaging/art.py

Writes what jpackage picks up from packaging/mac and packaging/win:

  mac/Fityesz Kronika-background.tiff   the drag-to-Applications window
  win/banner.bmp, win/dialog.bmp        the wizard's banner and side panel

The DMG background has to agree with the window geometry in the setup script
next to it: the icons sit where the script puts them, and the arrow is painted
between those two spots.
"""
from PIL import Image, ImageDraw, ImageFont
import pathlib
import subprocess

HERE = pathlib.Path(__file__).resolve().parent
FONTS = HERE.parent / 'src/main/resources/com/example/demo/fonts'
ART = HERE.parent / 'src/main/resources/com/example/demo/fityesz_art/opening'

PAPER = (244, 239, 230)
INK = (22, 22, 22)
RED = (200, 16, 46)
GREEN = (31, 107, 58)
GRAY = (74, 70, 64)
RULE = (207, 198, 182)

# The DMG window's content area, and where its two icons stand.
DMG = (600, 400)
APP_AT = (150, 205)
APPS_AT = (450, 205)


def font(name, size):
    return ImageFont.truetype(str(FONTS / name), size)


def halftone(img, scale, alpha=16):
    """The site's dot texture: ink at a whisper, on a 7px grid."""
    dots = Image.new('RGBA', img.size, (0, 0, 0, 0))
    pen = ImageDraw.Draw(dots)
    step = 7 * scale
    r = max(1, round(1.1 * scale))
    for y in range(0, img.size[1], step):
        for x in range(0, img.size[0], step):
            pen.ellipse([x - r, y - r, x + r, y + r], fill=INK + (alpha,))
    img.alpha_composite(dots)


def centred(pen, text, f, y, fill, width):
    w = pen.textbbox((0, 0), text, font=f)[2]
    pen.text(((width - w) / 2, y), text, font=f, fill=fill)


def spaced(text, gap=' '):
    return gap.join(text)


def background(scale):
    """The drag-to-Applications window, at 1x or 2x."""
    w, h = DMG[0] * scale, DMG[1] * scale
    img = Image.new('RGBA', (w, h), PAPER + (255,))
    halftone(img, scale)
    pen = ImageDraw.Draw(img)

    # the tricolour, along the top edge
    bar = 8 * scale
    pen.rectangle([0, 0, w // 3, bar], fill=RED)
    pen.rectangle([w // 3, 0, 2 * w // 3, bar], fill=(255, 255, 255))
    pen.rectangle([2 * w // 3, 0, w, bar], fill=GREEN)

    centred(pen, 'FITYESZ', font('Antonio-Bold.ttf', 46 * scale), 34 * scale, INK, w)
    centred(pen, spaced('KRÓNIKA'), font('Antonio-Bold.ttf', 17 * scale), 88 * scale, RED, w)

    # the arrow, from the app to the folder, between the two icon slots
    y = APP_AT[1] * scale
    x1, x2 = (APP_AT[0] + 108) * scale, (APPS_AT[0] - 108) * scale
    pen.line([x1, y, x2 - 10 * scale, y], fill=RED, width=3 * scale)
    pen.polygon(
        [(x2, y), (x2 - 16 * scale, y - 9 * scale), (x2 - 16 * scale, y + 9 * scale)],
        fill=RED,
    )

    centred(pen, 'Húzd az ikont az Applications mappába',
            font('PublicSans-Regular.ttf', 15 * scale), 306 * scale, INK, w)
    centred(pen, 'Drag the icon into Applications',
            font('PublicSans-Italic.ttf', 13 * scale), 330 * scale, GRAY, w)

    pen.line([70 * scale, 362 * scale, (w - 70 * scale), 362 * scale], fill=RULE, width=scale)
    centred(pen, spaced('FITYESZTHEGAME.COM'),
            font('PublicSans-Regular.ttf', 9 * scale), 372 * scale, GRAY, w)

    return img.convert('RGB')


def banner():
    """WiX draws the page title over the left of this strip, so keep it clear."""
    w, h = 493, 58
    img = Image.new('RGBA', (w, h), PAPER + (255,))
    halftone(img, 1, alpha=12)
    pen = ImageDraw.Draw(img)

    pen.rectangle([0, h - 4, w // 3, h], fill=RED)
    pen.rectangle([w // 3, h - 4, 2 * w // 3, h], fill=(255, 255, 255))
    pen.rectangle([2 * w // 3, h - 4, w, h], fill=GREEN)

    mark = Image.open(ART.parent.parent / 'fityesz_art/opening/opening_fityesz.png').convert('RGBA')
    mark = mark.crop((275, 318, 692, 735)).resize((38, 38), Image.LANCZOS)
    img.alpha_composite(mark, (w - 54, 9))

    f = font('Antonio-Bold.ttf', 20)
    pen.text((w - 54 - 8 - pen.textbbox((0, 0), 'FITYESZ KRÓNIKA', font=f)[2], 17),
             'FITYESZ KRÓNIKA', font=f, fill=INK)

    return img.convert('RGB')


def dialog():
    """The welcome and finish pages. WiX writes over the right two thirds."""
    w, h = 493, 312
    img = Image.new('RGBA', (w, h), (255, 255, 255, 255))
    pen = ImageDraw.Draw(img)

    panel = 164
    pen.rectangle([0, 0, panel, h], fill=PAPER)
    halftone(img.crop((0, 0, panel, h)).convert('RGBA'), 1)
    pen.rectangle([panel - 2, 0, panel, h], fill=INK)

    pen.rectangle([0, 0, panel // 3, 6], fill=RED)
    pen.rectangle([panel // 3, 0, 2 * panel // 3, 6], fill=(255, 255, 255))
    pen.rectangle([2 * panel // 3, 0, panel, 6], fill=GREEN)

    mark = Image.open(ART.parent.parent / 'fityesz_art/opening/opening_fityesz.png').convert('RGBA')
    mark = mark.crop((275, 318, 692, 735)).resize((74, 74), Image.LANCZOS)
    img.alpha_composite(mark, (45, 70))

    f = font('Antonio-Bold.ttf', 26)
    pen.text((45, 166), 'FITYESZ', font=f, fill=INK)
    pen.text((45, 196), spaced('KRÓNIKA'), font=font('Antonio-Bold.ttf', 11), fill=RED)

    pen.text((45, 240), 'Politikai', font=font('PublicSans-Regular.ttf', 12), fill=GRAY)
    pen.text((45, 256), 'kalandjáték', font=font('PublicSans-Regular.ttf', 12), fill=GRAY)

    return img.convert('RGB')


def main():
    (HERE / 'mac').mkdir(exist_ok=True)
    (HERE / 'win').mkdir(exist_ok=True)

    one, two = background(1), background(2)
    one.save(HERE / 'mac/background.png')
    two.save(HERE / 'mac/background@2x.png')

    # One TIFF carrying both sizes, which is how a Retina DMG background ships.
    subprocess.run(
        ['tiffutil', '-cathidpicheck', str(HERE / 'mac/background.png'),
         str(HERE / 'mac/background@2x.png'), '-out',
         str(HERE / 'mac/Fityesz Kronika-background.tiff')],
        check=True, capture_output=True,
    )

    banner().save(HERE / 'win/banner.bmp')
    dialog().save(HERE / 'win/dialog.bmp')
    print('írva: mac/Fityesz Kronika-background.tiff, win/banner.bmp, win/dialog.bmp')


if __name__ == '__main__':
    main()
