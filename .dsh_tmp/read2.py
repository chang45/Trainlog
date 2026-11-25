# -*- coding: utf-8 -*-
from docx import Document

doc = Document(r"C:\Users\17678\Desktop\数维杯论文书写模板.docx")
out = []
out.append("=== 表格 ===")
for i, tb in enumerate(doc.tables):
    out.append(f"table{i}: rows={len(tb.rows)} cols={len(tb.columns)} style={tb.style.name}")
    for ri, row in enumerate(tb.rows):
        out.append(f"  row{ri}: " + " | ".join(c.text for c in row.cells))
out.append("=== 正文标题(含'符号'或'说明') ===")
for p in doc.paragraphs:
    t = p.text.strip()
    if '符号' in t or '说明' in t or '目录' in t or '模型假设' in t:
        out.append(repr(t))
with open(r"E:\TrainLog\.dsh_tmp\desktop2.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(out))
print("ok")
