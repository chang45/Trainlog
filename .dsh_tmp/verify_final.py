# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Mm
from docx.oxml.ns import qn

doc = Document(r'E:\TrainLog\数维杯论文模板_最终版.docx')
out = []

s = doc.sections[0]
out.append(f"页面: {round(s.page_width/Mm(1),1)} x {round(s.page_height/Mm(1),1)} mm (A4=210x297)")
out.append(f"页眉: {s.header.paragraphs[0].text!r}")

# 样式
for name in ['Normal', 'Heading 1', 'Heading 2', 'Heading 3']:
    st = doc.styles[name]
    ea = st.element.rPr.rFonts.get(qn('w:eastAsia')) if st.element.rPr is not None and st.element.rPr.rFonts is not None else '?'
    out.append(f"样式 {name}: eastAsia={ea}, size={st.font.size.pt if st.font.size else '?'}, bold={st.font.bold}")

# 标题段落数
from collections import Counter
c = Counter(p.style.name for p in doc.paragraphs if p.style and p.style.name.startswith('Heading'))
out.append(f"标题段落: {dict(c)}")

# TOC 域
body_xml = doc.element.body.xml
out.append(f"TOC域: {'有' if 'TOC' in body_xml and 'fldChar' in body_xml else '无'}")

# 三线表
tb = doc.tables[0]
tblPr = tb._tbl.tblPr
b = tblPr.find(qn('w:tblBorders'))
if b is not None:
    top = b.find(qn('w:top')).get(qn('w:val'))
    bot = b.find(qn('w:bottom')).get(qn('w:val'))
    left = b.find(qn('w:left')).get(qn('w:val'))
    out.append(f"三线表: top={top}, bottom={bot}, left={left} (应为 single/single/none)")
out.append(f"表格: {len(tb.rows)}x{len(tb.columns)}, 表头={[c.text for c in tb.rows[0].cells]}")

with open(r'E:\TrainLog\.dsh_tmp\verify_final.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))
print('ok')
