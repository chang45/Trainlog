# -*- coding: utf-8 -*-
from docx import Document

doc = Document(r"C:\Users\17678\Desktop\数维杯论文书写模板.docx")
print("paragraphs:", len(doc.paragraphs), "tables:", len(doc.tables))

lines = []
for p in doc.paragraphs:
    if p.text.strip():
        lines.append(p.text)
with open(r"E:\TrainLog\.dsh_tmp\desktop_docx.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print("non-empty paras:", len(lines))
print("=== tables ===")
for i, tb in enumerate(doc.tables):
    print(f"table {i}: rows={len(tb.rows)} cols={len(tb.columns)} style={tb.style.name if tb.style else '?'}")
    for row in tb.rows[:6]:
        print("   ", [c.text for c in row.cells])
