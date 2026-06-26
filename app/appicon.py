from copy import deepcopy
from pathlib import Path
from lxml import etree
import cairosvg

INKSCAPE = "http://www.inkscape.org/namespaces/inkscape"
SVG = "http://www.w3.org/2000/svg"

NS = {
    "inkscape": INKSCAPE,
    "svg": SVG,
}

BLACK = "#000000"
WHITE = "#ffffff"

PAINTABLE_TAGS = {
    f"{{{SVG}}}path",
    f"{{{SVG}}}ellipse",
    f"{{{SVG}}}circle",
    f"{{{SVG}}}rect",
    f"{{{SVG}}}polygon",
    f"{{{SVG}}}polyline",
    f"{{{SVG}}}line",
}


def parse_style(style_text):
    style = {}

    if not style_text:
        return style

    for part in style_text.split(";"):
        if ":" in part:
            key, value = part.split(":", 1)
            style[key.strip()] = value.strip()

    return style


def style_to_string(style):
    return ";".join(f"{key}:{value}" for key, value in style.items())


def update_style(element, **updates):
    style = parse_style(element.get("style"))

    for key, value in updates.items():
        key = key.replace("_", "-")

        if value is None:
            style.pop(key, None)
        else:
            style[key] = value

    if style:
        element.set("style", style_to_string(style))
    elif "style" in element.attrib:
        del element.attrib["style"]


def find_by_inkscape_label(root, label):
    matches = root.xpath(
        '//*[@inkscape:label=$label]',
        namespaces=NS,
        label=label,
    )

    if not matches:
        raise ValueError(f'Could not find element with inkscape:label="{label}"')

    return matches[0]


def set_foreground_color(element, color):
    """
    Sets fill color both as a presentation attribute and inside style="...".
    This matters because style fill usually overrides fill="...".
    """
    element.set("fill", color)

    update_style(
        element,
        fill=color,
        fill_opacity="1",
    )


def uniquify_ids(element, suffix):
    """
    Prevent duplicated IDs after cloning SVG nodes.
    """
    if element.get("id"):
        element.set("id", element.get("id") + suffix)

    for child in element:
        uniquify_ids(child, suffix)


def remove_generated_outline(root, outline_id):
    for old in root.xpath('//*[@id=$outline_id]', outline_id=outline_id):
        parent = old.getparent()
        if parent is not None:
            parent.remove(old)


def add_outline_behind_single_element(
        element,
        outline_id,
        stroke_color=WHITE,
        stroke_width="3",
):
    """
    Clone one element, turn the clone into a stroke-only outline,
    and place it behind the original.
    """
    parent = element.getparent()

    if parent is None:
        raise ValueError("Element has no parent; cannot insert outline behind it.")

    insert_index = parent.index(element)

    outline = deepcopy(element)
    uniquify_ids(outline, suffix="-outline")
    outline.set("id", outline_id)
    outline.set(f"{{{INKSCAPE}}}label", outline_id)

    outline.attrib.pop("fill", None)
    outline.attrib.pop("stroke", None)
    outline.attrib.pop("stroke-width", None)

    update_style(
        outline,
        fill="none",
        fill_opacity="0",
        stroke=stroke_color,
        stroke_width=str(stroke_width),
        stroke_linejoin="round",
        stroke_linecap="round",
        stroke_opacity="1",
    )

    parent.insert(insert_index, outline)
    return outline


def add_outline_behind_group(
        group,
        outline_id,
        stroke_color=WHITE,
        stroke_width="4",
):
    """
    Clone a whole group, turn each drawable child into a stroke-only outline,
    and place the clone behind the original group.

    This is the right approach for the 'pin' group because its child elements
    overlap. The original filled group hides internal stroke overlaps.
    """
    parent = group.getparent()

    if parent is None:
        raise ValueError("Group has no parent; cannot insert outline behind it.")

    insert_index = parent.index(group)

    outline_group = deepcopy(group)
    uniquify_ids(outline_group, suffix="-outline")
    outline_group.set("id", outline_id)
    outline_group.set(f"{{{INKSCAPE}}}label", outline_id)

    for el in outline_group.iter():
        if el.tag not in PAINTABLE_TAGS:
            continue

        for attr in (
                "fill",
                "stroke",
                "stroke-width",
                "stroke-linejoin",
                "stroke-linecap",
        ):
            el.attrib.pop(attr, None)

        update_style(
            el,
            fill="none",
            fill_opacity="0",
            stroke=stroke_color,
            stroke_width=str(stroke_width),
            stroke_linejoin="round",
            stroke_linecap="round",
            stroke_opacity="1",
        )

    parent.insert(insert_index, outline_group)
    return outline_group


def transform_logo_svg(
        root,
        *,
        pin_outline_width="4",
        lightning_outline_width="3",
):
    """
    Mutates an already-loaded SVG root in place.

    Required SVG labels:
      - element-middle
      - element-top
      - element-bottom
      - lightning
      - shadow
      - paper
      - bent
      - pin
    """

    black_labels = {
        "element-middle",
        "element-top",
        "element-bottom",
        "lightning",
    }

    white_labels = {
        "paper",
        "bent",
        "shadow",
    }

    # Make function safe to rerun on the same SVG.
    remove_generated_outline(root, "pin-outline")
    remove_generated_outline(root, "lightning-outline")

    # Recolor foreground elements.
    for label in black_labels:
        element = find_by_inkscape_label(root, label)
        set_foreground_color(element, BLACK)

    for label in white_labels:
        element = find_by_inkscape_label(root, label)
        set_foreground_color(element, WHITE)

    # Add white outlines.
    pin_group = find_by_inkscape_label(root, "pin")
    lightning = find_by_inkscape_label(root, "lightning")

    add_outline_behind_group(
        pin_group,
        outline_id="pin-outline",
        stroke_color=WHITE,
        stroke_width=pin_outline_width,
    )

    add_outline_behind_single_element(
        lightning,
        outline_id="lightning-outline",
        stroke_color=WHITE,
        stroke_width=lightning_outline_width,
    )

    return root
