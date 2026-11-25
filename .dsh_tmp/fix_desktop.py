# -*- coding: utf-8 -*-
from docx import Document
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

path = r"C:\Users\17678\Desktop\数维杯论文书写模板.docx"
doc = Document(path)

# 1. 修正正文标题 "符号说明" -> "四、符号说明"
for p in doc.paragraphs:
    if p.text.strip() == '符号说明':
        for r in p.runs:
            r.text = ''
        if p.runs:
            p.runs[0].text = '四、符号说明'
        else:
            p.add_run('四、符号说明')
        break


def three_line(table):
    tbl = table._tbl
    tblPr = tbl.tblPr
    for old in tblPr.findall(qn('w:tblBorders')):
        tblPr.remove(old)
    borders = OxmlElement('w:tblBorders')

    def edge(name, val, sz):
        el = OxmlElement(f'w:{name}')
        el.set(qn('w:val'), val)
        el.set(qn('w:sz'), str(sz))
        el.set(qn('w:space'), '0')
        el.set(qn('w:color'), '000000')
        return el

    borders.append(edge('top', 'single', 12))     # 顶线 粗
    borders.append(edge('bottom', 'single', 12))  # 底线 粗
    borders.append(edge('left', 'none', 0))
    borders.append(edge('right', 'none', 0))
    borders.append(edge('insideH', 'none', 0))
    borders.append(edge('insideV', 'none', 0))
    tblPr.append(borders)

    # 表头行底部细线
    for cell in table.rows[0].cells:
        tcPr = cell._tc.get_or_add_tcPr()
        for old in tcPr.findall(qn('w:tcBorders')):
            tcPr.remove(old)
        tcBorders = OxmlElement('w:tcBorders')
        btm = OxmlElement('w:bottom')
        btm.set(qn('w:val'), 'single')
        btm.set(qn('w:sz'), '6')
        btm.set(qn('w:space'), '0')
        btm.set(qn('w:color'), '000000')
        tcBorders.append(btm)
        tcPr.append(tcBorders)


for tb in doc.tables:
    three_line(tb)

doc.save(path)
print('fixed')
