"""Turn a client.record folder into an animated GIF (or WebP), with the virtual pointer drawn in.

The game's framebuffer never holds a mouse cursor, since the system draws the real one, so the frames
client.record writes have none. It writes where the agent's virtual pointer was beside every frame in
frames.jsonl instead, and this draws a cursor there: a plain arrow, the same whether a button is
down or not.

    python tools/agent/make_gif.py run/26.3-fabric/agent/client/screenshots/backpack-showcase build/backpack-showcase.gif

Options:
    --width N        output width in pixels; by default the frame's own, which keeps the GUI's
                     pixels whole. Any other width blurs the text and multiplies its colours
    --crop X,Y,W,H   crop first, in the frames' own pixels (frames.jsonl's frameWidth, frameHeight)
    --from N --to N  keep only frames N..N (1-based, as frames.jsonl numbers them)
    --speed F        play F times faster than the game ran; 1 by default
    --hold MS        how long the last frame stays before the loop starts over; 1500 by default
    --cursor F       the cursor's size in output pixels per arrow pixel, 1.5 by default: an
                     18 by 28 arrow, about a system cursor's size on a 1024 wide frame
    --dither         dither the world behind the screen rather than mapping it to the nearest colour:
                     smoother, and three to four times the size, since the dither changes every frame

A GIF holds 256 colours, one palette shared by every frame so nothing flickers. Half of it is cut
from the pixels, which is where most of the picture is: the world behind the screen and the panels.
The other half goes one entry at a time to whichever colour of the recording is furthest from every
entry so far, which is what keeps an icon's dozen red pixels red. Cut from the pixels alone, the
apple came out brown and the highlighted row's orange pink.

The output's extension picks the format: .gif, or .webp for a full colour file, with no palette at all, that
GitHub and Modrinth both show. Needs Pillow and NumPy.

Consecutive frames that came out identical are merged into one longer frame, so a script that waits
on a still screen costs nothing in file size.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import numpy
    from PIL import Image, ImageChops, ImageDraw
except ImportError:  # pragma: no cover - a clear message beats a traceback
    sys.exit("make_gif.py needs Pillow and NumPy: pip install pillow numpy")

# The arrow, one character a pixel: '#' outline, '.' fill, anything else clear. Tip at (0, 0).
ARROW = [
    "#",
    "##",
    "#.#",
    "#..#",
    "#...#",
    "#....#",
    "#.....#",
    "#......#",
    "#.......#",
    "#........#",
    "#.........#",
    "#......#####",
    "#...#..#",
    "#..##..#",
    "#.#  #..#",
    "##   #..#",
    "#     #..#",
    "      #..#",
    "       ##",
]


def read_recording(folder: Path) -> tuple[dict, list[dict]]:
    lines = [json.loads(line) for line in (folder / "frames.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    if not lines:
        sys.exit(f"{folder / 'frames.jsonl'} is empty")
    return lines[0], lines[1:]


_cursors: dict[float, Image.Image] = {}


def cursor(size: float) -> Image.Image:
    """The arrow at {size} output pixels an arrow pixel, drawn large and scaled down so any size stays crisp."""
    if size not in _cursors:
        big = 8
        sprite = Image.new("RGBA", (12 * big, len(ARROW) * big), (0, 0, 0, 0))
        draw = ImageDraw.Draw(sprite)
        for row, text in enumerate(ARROW):
            for column, pixel in enumerate(text):
                if pixel in "#.":
                    colour = (0, 0, 0, 255) if pixel == "#" else (255, 255, 255, 255)
                    draw.rectangle((column * big, row * big, column * big + big - 1, row * big + big - 1), fill=colour)
        width = max(1, round(12 * size))
        _cursors[size] = sprite.resize((width, max(1, round(len(ARROW) * size))), Image.LANCZOS)
    return _cursors[size]


def draw_cursor(image: Image.Image, x: float, y: float, size: float) -> None:
    sprite = cursor(size)
    image.paste(sprite, (round(x), round(y)), sprite)


def build_frames(folder: Path, header: dict, frames: list[dict], args) -> list[tuple[Image.Image, int]]:
    crop = tuple(int(v) for v in args.crop.split(",")) if args.crop else None
    # A frame stays up for the ticks that passed before the next one, at 50 ms a tick.
    ticks = [line["tick"] for line in frames] + [header.get("ticks", frames[-1]["tick"] + header.get("every", 2))]
    out: list[tuple[Image.Image, float]] = []
    for index, line in enumerate(frames):
        ms = max(1, ticks[index + 1] - ticks[index]) * 50 / args.speed
        number = line["frame"]
        if number < args.start or (args.end and number > args.end):
            continue
        path = folder / line["file"]
        if not path.is_file():
            print(f"skipping frame {number}: {path.name} is not there", file=sys.stderr)
            continue
        image = Image.open(path).convert("RGB")
        x, y = line.get("x"), line.get("y")
        if crop:
            image = image.crop((crop[0], crop[1], crop[0] + crop[2], crop[1] + crop[3]))
            if x is not None:
                x, y = x - crop[0], y - crop[1]
        factor = 1.0
        if args.width and image.width != args.width:
            factor = args.width / image.width
            image = image.resize((args.width, round(image.height * factor)), Image.LANCZOS)
        if line.get("pointer") and x is not None:
            draw_cursor(image, x * factor, y * factor, args.cursor)
        if out and ImageChops.difference(out[-1][0], image).getbbox() is None:
            out[-1] = (out[-1][0], out[-1][1] + ms)
        else:
            out.append((image, ms))
    if not out:
        sys.exit("no frames to write")
    last, duration = out[-1]
    out[-1] = (last, duration + args.hold)
    return [(image, round(duration)) for image, duration in out]


def shared_palette(images: list[Image.Image], base: int = 128, colours: int = 256) -> Image.Image:
    """One palette for every frame, which leaves no colour of the recording far from an entry."""
    # Every colour of the recording, with the largest count it reaches in any one frame. A colour that
    # never covers three pixels of a frame is the edge of something moving, not a colour to keep.
    largest: dict[tuple, int] = {}
    for image in images:
        for count, colour in image.getcolors(image.width * image.height):
            if count > largest.get(colour, 0):
                largest[colour] = count
    candidates = numpy.array([colour for colour, count in largest.items() if count >= 3], dtype=numpy.float64)

    # Half the palette cut from the pixels themselves, which spends it where most of the picture is:
    # the world behind the screen and the panels over it.
    step = max(1, len(images) // 24)
    sample_pixels = [pixel for image in images[::step]
                     for pixel in image.resize((image.width // 4, image.height // 4)).get_flattened_data()]
    sample = Image.new("RGB", (len(sample_pixels), 1))
    sample.putdata(sample_pixels)
    cut = sample.quantize(colors=base, method=Image.Quantize.MEDIANCUT).getpalette()[:base * 3]
    entries = [tuple(cut[i:i + 3]) for i in range(0, len(cut), 3)]
    # The cursor make_gif.py draws, whatever the frames hold.
    entries += [c for c in ((0, 0, 0), (255, 255, 255), (200, 200, 200)) if c not in entries]

    # The other half, one entry at a time, to whichever colour is furthest from every entry so far.
    # That is what a pixel count never gives the few pixels of an icon: the apple's red and the water's
    # blue, a dozen pixels each, came out brown and grey from a palette cut from pixels alone.
    weights = numpy.array([2.0, 4.0, 3.0])
    nearest = numpy.full(len(candidates), numpy.inf)
    for entry in entries:
        nearest = numpy.minimum(nearest, (((candidates - entry) ** 2) * weights).sum(axis=1))
    while len(entries) < colours and len(candidates):
        furthest = int(nearest.argmax())
        if nearest[furthest] == 0:
            break
        entry = tuple(int(v) for v in candidates[furthest])
        entries.append(entry)
        nearest = numpy.minimum(nearest, (((candidates - entry) ** 2) * weights).sum(axis=1))

    palette = Image.new("P", (1, 1))
    palette.putpalette([channel for colour in entries for channel in colour] + [0] * (768 - 3 * len(entries)))
    return palette


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("folder", type=Path, help="the recording: screenshots/<name> beside the queue")
    parser.add_argument("output", type=Path, help="the .gif or .webp to write")
    parser.add_argument("--width", type=int, default=0)
    parser.add_argument("--crop")
    parser.add_argument("--from", dest="start", type=int, default=1)
    parser.add_argument("--to", dest="end", type=int, default=0)
    parser.add_argument("--speed", type=float, default=1.0)
    parser.add_argument("--hold", type=int, default=1500)
    parser.add_argument("--cursor", type=float, default=1.5)
    parser.add_argument("--dither", action="store_true")
    args = parser.parse_args()

    header, frames = read_recording(args.folder)
    built = build_frames(args.folder, header, frames, args)
    images = [image for image, _ in built]
    durations = [duration for _, duration in built]
    args.output.parent.mkdir(parents=True, exist_ok=True)

    if args.output.suffix.lower() == ".webp":
        images[0].save(args.output, save_all=True, append_images=images[1:], duration=durations,
                       loop=0, quality=90, method=6)
    else:
        palette = shared_palette(images)
        dither = Image.Dither.FLOYDSTEINBERG if args.dither else Image.Dither.NONE
        indexed = [image.quantize(palette=palette, dither=dither) for image in images]
        indexed[0].save(args.output, save_all=True, append_images=indexed[1:], duration=durations,
                        loop=0, optimize=True, disposal=1)

    size = args.output.stat().st_size
    total = sum(durations) / 1000
    print(f"{args.output}: {len(images)} frames from {len(frames)}, {total:.1f} s, "
          f"{images[0].width}x{images[0].height}, {size / 1024:.0f} KiB")


if __name__ == "__main__":
    main()
