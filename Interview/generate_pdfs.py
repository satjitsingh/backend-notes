import os
import markdown
from xhtml2pdf import pisa

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(BASE_DIR, "PDFs")
os.makedirs(OUTPUT_DIR, exist_ok=True)

CSS = """
@page {
    size: A4;
    margin: 2cm 1.8cm;
    @frame footer {
        -pdf-frame-content: footerContent;
        bottom: 0.5cm;
        margin-left: 1.8cm;
        margin-right: 1.8cm;
        height: 1cm;
    }
}

body {
    font-family: Helvetica, Arial, sans-serif;
    font-size: 11px;
    color: #1a1a1a;
    line-height: 1.65;
}

h1 {
    font-size: 22px;
    color: #111;
    border-bottom: 2.5px solid #222;
    padding-bottom: 8px;
    margin-top: 24px;
    margin-bottom: 16px;
    font-weight: 700;
    letter-spacing: 0.3px;
}

h2 {
    font-size: 17px;
    color: #1a1a1a;
    margin-top: 28px;
    margin-bottom: 10px;
    padding-bottom: 5px;
    border-bottom: 1.5px solid #ccc;
    font-weight: 600;
}

h3 {
    font-size: 14px;
    color: #2a2a2a;
    margin-top: 20px;
    margin-bottom: 8px;
    font-weight: 600;
}

h4 {
    font-size: 12px;
    color: #333;
    margin-top: 14px;
    margin-bottom: 6px;
    font-weight: 600;
}

p {
    margin-bottom: 8px;
    text-align: justify;
}

code {
    font-family: Courier, monospace;
    font-size: 10px;
    background-color: #f0f0f0;
    padding: 1px 4px;
    border: 0.5px solid #ddd;
}

pre {
    background-color: #f5f5f5;
    border: 1px solid #ddd;
    border-left: 3.5px solid #444;
    padding: 12px 14px;
    margin: 10px 0 14px 0;
    font-family: Courier, monospace;
    font-size: 9.5px;
    line-height: 1.5;
    overflow-x: auto;
    white-space: pre-wrap;
    word-wrap: break-word;
}

pre code {
    background: none;
    border: none;
    padding: 0;
    font-size: 9.5px;
}

blockquote {
    border-left: 3.5px solid #888;
    margin: 12px 0;
    padding: 8px 16px;
    background-color: #fafafa;
    color: #444;
    font-style: italic;
}

table {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 10px;
}

th {
    background-color: #3a3a3a;
    color: #ffffff;
    font-weight: 600;
    padding: 8px 10px;
    text-align: left;
    border: 1px solid #333;
    font-size: 10px;
}

td {
    padding: 7px 10px;
    border: 1px solid #ccc;
    text-align: left;
    vertical-align: top;
}

tr:nth-child(even) td {
    background-color: #f7f7f7;
}

ul, ol {
    margin: 6px 0 10px 20px;
    padding-left: 10px;
}

li {
    margin-bottom: 4px;
}

strong {
    font-weight: 700;
    color: #111;
}

em {
    font-style: italic;
}

hr {
    border: none;
    border-top: 1.5px solid #ddd;
    margin: 20px 0;
}

a {
    color: #1a5276;
    text-decoration: none;
}
"""

MD_FILES = [
    os.path.join(BASE_DIR, "Backend_Interview_Roadmap.md"),
    os.path.join(BASE_DIR, "Paytm", "01_DSA_Questions.md"),
    os.path.join(BASE_DIR, "Paytm", "02_Java_SpringBoot_DeepDive.md"),
    os.path.join(BASE_DIR, "Paytm", "03_LLD_and_Concurrency.md"),
    os.path.join(BASE_DIR, "Paytm", "04_Databases_and_Kafka.md"),
    os.path.join(BASE_DIR, "Paytm", "05_Amdocs_Transition_and_Behavioral.md"),
]

md_extensions = ["tables", "fenced_code", "codehilite", "toc", "nl2br"]

def convert_md_to_pdf(md_path, output_dir):
    filename = os.path.splitext(os.path.basename(md_path))[0]
    pdf_path = os.path.join(output_dir, f"{filename}.pdf")

    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()

    html_body = markdown.markdown(md_text, extensions=md_extensions)

    html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>{CSS}</style>
</head>
<body>
{html_body}
<div id="footerContent" style="text-align: center; font-size: 8px; color: #999;">
    {filename.replace('_', ' ')}
</div>
</body>
</html>"""

    with open(pdf_path, "wb") as pdf_file:
        status = pisa.CreatePDF(html, dest=pdf_file)

    if status.err:
        print(f"  FAILED: {filename} ({status.err} errors)")
        return False
    else:
        size_kb = os.path.getsize(pdf_path) / 1024
        print(f"  OK: {pdf_path} ({size_kb:.0f} KB)")
        return True


def main():
    print("=" * 60)
    print("  Generating Print-Ready PDFs")
    print("=" * 60)
    print(f"Output folder: {OUTPUT_DIR}\n")

    success = 0
    failed = 0

    for md_file in MD_FILES:
        if not os.path.exists(md_file):
            print(f"  SKIP: {md_file} (not found)")
            failed += 1
            continue
        name = os.path.basename(md_file)
        print(f"  Converting: {name}")
        if convert_md_to_pdf(md_file, OUTPUT_DIR):
            success += 1
        else:
            failed += 1

    print(f"\nDone! {success} PDFs generated, {failed} failed.")
    print(f"Location: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
