import numpy as np
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
import os
from lxml import etree

import cairosvg

import appicon

# ============================================================
# Hard-coded config
# ============================================================

SVG_FILE_PATH = "logo_source.svg"
TICKER_BAR_FILE_PATH = "notification_ticker_bar.svg"
RES_DIR_PATH = f"src{os.sep}main{os.sep}res"

DENSITY_BUCKETS = {
    # "drawable-ldpi": 0.75,
    "drawable-mdpi": 1.0,
    "drawable-hdpi": 1.5,
    "drawable-xhdpi": 2.0,
    "drawable-xxhdpi": 3.0,
    "drawable-xxxhdpi": 4.0,
    "raw": 20.0,
}

# ============================================================
# SVG size helpers
# ============================================================

_LENGTH_RE = re.compile(r"^\s*([0-9]*\.?[0-9]+)\s*([a-zA-Z%]*)\s*$")

UNIT_TO_PX = {
    "": 1.0,
    "px": 1.0,
    "dp": 1.0,
    "dip": 1.0,
    "pt": 96.0 / 72.0,
    "pc": 16.0,
    "in": 96.0,
    "cm": 96.0 / 2.54,
    "mm": 96.0 / 25.4,
}


def parse_length(value: str | None) -> float | None:
    if not value:
        return None

    match = _LENGTH_RE.match(value)
    if not match:
        return None

    number = float(match.group(1))
    unit = match.group(2).lower()

    if unit == "%":
        return None

    if unit not in UNIT_TO_PX:
        return None

    return number * UNIT_TO_PX[unit]


def get_svg_mdpi_size(svg_path: Path) -> tuple[int, int]:
    tree = ET.parse(svg_path)
    root = tree.getroot()

    width = parse_length(root.get("width"))
    height = parse_length(root.get("height"))

    if width and height:
        return round(width), round(height)

    viewbox = root.get("viewBox")
    if viewbox:
        parts = re.split(r"[,\s]+", viewbox.strip())
        if len(parts) == 4:
            _, _, vb_width, vb_height = map(float, parts)
            return round(vb_width), round(vb_height)

    raise ValueError(
        "Could not determine SVG size. Add width/height or viewBox to the SVG!"
    )


# ============================================================
# Main Entry
# ============================================================

def main() -> None:
    svg_path = Path(SVG_FILE_PATH)
    res_dir = Path(RES_DIR_PATH)

    if not svg_path.is_file():
        raise FileNotFoundError(f"SVG file not found: {svg_path}")

    if not res_dir.is_dir():
        raise NotADirectoryError(f"Android res directory not found: {res_dir}")

    mdpi_width, mdpi_height = get_svg_mdpi_size(svg_path)

    print(f"Input SVG: {svg_path}")
    print(f"mdpi baseline: {mdpi_width}x{mdpi_height}")
    print()

    ####################################################################
    # Saving enabled / disabled buttons at different resolutions
    ####################################################################

    for folder_name, scale in DENSITY_BUCKETS.items():
        output_dir = res_dir / folder_name
        output_dir.mkdir(parents=True, exist_ok=True)

        output_width = max(1, round(mdpi_width * scale * 0.75))
        output_height = max(1, round(mdpi_height * scale * 0.75))
        output_path = f"{output_dir}{os.sep}enabled_button.png"
        output_path = os.path.abspath(str(output_path))

        # Scaling logo so it can be used as 'enabled_button.png'
        cairosvg.svg2png(
            url=str(svg_path),
            write_to=output_path,
            output_width=output_width,
            output_height=output_height,
        )

        # Reading logo again and turning it to black & white
        rgb_image = Image.open(output_path)
        rgb_image = np.asarray(rgb_image)

        gray = np.dot(rgb_image[..., :3], [0.25415, 0.49895, 0.09690])
        gray_rgba = np.dstack([
            gray,
            gray,
            gray,
            rgb_image[..., 3]
        ]).astype(rgb_image.dtype)

        output_path = f"{output_path[:-18]}disabled_button.png"
        gray_rgba = Image.fromarray(gray_rgba)
        gray_rgba.save(output_path)

        # Done.
        print(f"Wrote {folder_name} -> {output_width}x{output_height}")

    ####################################################################
    # Generating the notification ticker bar icon
    ####################################################################

    # first we transform the base icon
    logo_to_notification_bar_icon()
    print('Drawer icon base generated.')

    for folder_name, scale in DENSITY_BUCKETS.items():
        output_dir = res_dir / folder_name
        output_dir.mkdir(parents=True, exist_ok=True)

        output_width = max(1, round(mdpi_width * scale * (2 / 3)))
        output_height = max(1, round(mdpi_height * scale * (2 / 3)))
        output_path = f"{output_dir}{os.sep}notification_ticker_bar.png"
        output_path = os.path.abspath(str(output_path))

        # Scaling logo so it can be used as 'enabled_button.png'
        cairosvg.svg2png(
            url=TICKER_BAR_FILE_PATH,
            write_to=output_path,
            output_width=output_width,
            output_height=output_height,
        )

        # Post processing. Treating black as the new alpha channel.
        rgb_image = Image.open(output_path)
        rgb_image = np.asarray(rgb_image)

        alpha = rgb_image[:, :, 3]
        avg_colors = np.zeros(shape=alpha.shape, dtype=np.double)
        avg_colors += rgb_image[:, :, 0]
        avg_colors += rgb_image[:, :, 1]
        avg_colors += rgb_image[:, :, 2]
        avg_colors = (avg_colors / 765) * 255
        avg_colors = avg_colors.astype(np.uint8)

        merged = np.where(alpha == 255, avg_colors, alpha).astype(np.uint8)
        rgba_image = np.ones(shape=(alpha.shape[0], alpha.shape[1], 4), dtype=np.uint8) * 255
        rgba_image[:, :, 3] = merged
        rgba_image = Image.fromarray(rgba_image)
        rgba_image.save(output_path)

        # Done.
        print(f"Wrote ticker {folder_name} -> {output_width}x{output_height}")

    print()
    print("Done.")


def logo_to_notification_bar_icon():
    tree = etree.parse("logo_source.svg")
    root = tree.getroot()

    appicon.transform_logo_svg(
        root,
        pin_outline_width="4",
        lightning_outline_width="4",
    )

    tree.write(
        TICKER_BAR_FILE_PATH,
        encoding="UTF-8",
        xml_declaration=True,
        pretty_print=True,
    )


if __name__ == "__main__":
    main()
