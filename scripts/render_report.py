#!/usr/bin/env python3
"""Render the report's small Markdown subset to a five-page A4 PDF."""

import re
import reportlab
from html import escape
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output/pdf/REPORT.pdf"
WIDTH = A4[0] - 84
FONT_DIR = Path(reportlab.__file__).parent / "fonts"
for name, filename in [("Report", "Vera.ttf"), ("ReportBold", "VeraBd.ttf"),
                       ("ReportItalic", "VeraIt.ttf")]:
    pdfmetrics.registerFont(TTFont(name, str(FONT_DIR / filename)))
styles = getSampleStyleSheet()
styles.add(ParagraphStyle("Body", fontName="Report", fontSize=9.5, leading=12.5,
                          spaceAfter=7))
styles.add(ParagraphStyle("Cell", parent=styles["Body"], fontSize=8, leading=10,
                          spaceAfter=0))
styles["Heading1"].fontSize = 17
styles["Heading1"].leading = 21
styles["Heading2"].fontSize = 13
styles["Heading2"].leading = 17
styles["Heading3"].fontSize = 10.5
styles["Heading3"].leading = 14
styles["Heading1"].fontName = "ReportBold"
styles["Heading2"].fontName = "ReportBold"
styles["Heading3"].fontName = "ReportBold"


def paragraph(text, style="Body"):
    text = escape(text)
    text = re.sub(r"`([^`]+)`", r'<font name="Courier">\1</font>', text)
    return Paragraph(text, styles[style])


def table(lines):
    rows = [[cell.strip() for cell in line.strip("|").split("|")] for line in lines]
    rows = [row for row in rows if not all(re.fullmatch(r":?-+:?", cell) for cell in row)]
    count = len(rows[0])
    if count == 4:
        widths = [0.18, 0.30, 0.19, 0.33]
    elif count == 3:
        widths = [0.28, 0.30, 0.42]
    elif count == 6:
        widths = [0.31] + [0.138] * 5
    else:
        widths = [1 / count] * count
    cells = [[paragraph(cell, "Cell") for cell in row] for row in rows]
    result = Table(cells, colWidths=[WIDTH * fraction for fraction in widths], repeatRows=1)
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e8eef4")),
        ("LINEBELOW", (0, 0), (-1, 0), 0.6, colors.HexColor("#7890a4")),
        ("LINEBELOW", (0, 1), (-1, -1), 0.25, colors.HexColor("#d0d7de")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return result


def footer(canvas, document):
    canvas.saveState()
    canvas.setFont("Report", 8)
    canvas.setFillColor(colors.HexColor("#526579"))
    canvas.drawString(42, 25, "DAA Assignment 3 | Graphs | 23 October 2026")
    canvas.drawRightString(A4[0] - 42, 25, str(document.page))
    canvas.restoreState()


def render():
    lines = (ROOT / "REPORT.md").read_text(encoding="utf-8").splitlines()
    story = []
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        index += 1
        if not line:
            continue
        if line == "<!-- pagebreak -->":
            story.append(PageBreak())
        elif line.startswith("#"):
            heading, text = line.split(" ", 1)
            story.append(paragraph(text, "Heading" + str(len(heading))))
        elif line.startswith("!["):
            path = re.fullmatch(r"!\[.*\]\((.*)\)", line).group(1)
            picture = Image(str(ROOT / path))
            picture.drawHeight *= WIDTH / picture.drawWidth
            picture.drawWidth = WIDTH
            story.extend([picture, Spacer(1, 5)])
        elif line.startswith("|"):
            group = [line]
            while index < len(lines) and lines[index].startswith("|"):
                group.append(lines[index])
                index += 1
            story.extend([table(group), Spacer(1, 8)])
        else:
            group = [line]
            while index < len(lines) and lines[index].strip():
                group.append(lines[index].strip())
                index += 1
            story.append(paragraph(" ".join(group)))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(str(OUTPUT), pagesize=A4, leftMargin=42, rightMargin=42,
                                 topMargin=35, bottomMargin=40, title="Assignment 3: Graph Algorithms",
                                 author="DAA Assignment 3 project")
    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    render()
