# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Pt, Cm, Mm, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.enum.text import WD_ALIGN_PARAGRAPH

BLACK = RGBColor(0, 0, 0)

doc = Document()

# ===== 页面 A4 =====
sec = doc.sections[0]
sec.page_width = Mm(210)
sec.page_height = Mm(297)
sec.top_margin = Cm(2.54)
sec.bottom_margin = Cm(2.54)
sec.left_margin = Cm(3.17)
sec.right_margin = Cm(3.17)

# ===== 页眉 =====
hp = sec.header.paragraphs[0]
hp.text = ''
hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
hr = hp.add_run('Team # SWBMCM2612781')
hr.font.name = 'Times New Roman'
hr.font.size = Pt(9)

# ===== 默认样式：宋体小四 =====
normal = doc.styles['Normal']
normal.font.name = 'Times New Roman'
normal.font.size = Pt(12)
normal.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
normal.font.color.rgb = BLACK


def set_heading_style(name, cn_font, size, bold):
    st = doc.styles[name]
    st.font.name = 'Times New Roman'
    st.font.size = Pt(size)
    st.font.bold = bold
    st.font.color.rgb = BLACK
    st.element.rPr.rFonts.set(qn('w:eastAsia'), cn_font)
    # 去掉默认的蓝色主题色
    if st.element.rPr is not None and st.element.rPr.rFonts is not None:
        st.element.rPr.rFonts.set(qn('w:ascii'), 'Times New Roman')
        st.element.rPr.rFonts.set(qn('w:hAnsi'), 'Times New Roman')


set_heading_style('Heading 1', '黑体', 14, True)   # 一级标题 黑体四号
set_heading_style('Heading 2', '黑体', 12, True)   # 二级标题 黑体小四加粗
set_heading_style('Heading 3', '黑体', 12, False)  # 三级标题 黑体小四


def cn(run, font='宋体'):
    run.font.name = 'Times New Roman'
    run._element.get_or_add_rPr().get_or_add_rFonts().set(qn('w:eastAsia'), font)


def title(text, size=16, bold=True):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(6)
    r = p.add_run(text)
    cn(r, '黑体')
    r.font.size = Pt(size)
    r.font.bold = bold
    r.font.color.rgb = BLACK
    return p


def h1(text):
    p = doc.add_heading(text, level=1)
    return p


def h2(text):
    return doc.add_heading(text, level=2)


def h3(text):
    return doc.add_heading(text, level=3)


def blank(n=1):
    for _ in range(n):
        p = doc.add_paragraph()
        p.paragraph_format.first_line_indent = Pt(24)
        p.paragraph_format.line_spacing = 1.5
        r = p.add_run('')
        cn(r, '宋体')


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

    borders.append(edge('top', 'single', 12))
    borders.append(edge('bottom', 'single', 12))
    borders.append(edge('left', 'none', 0))
    borders.append(edge('right', 'none', 0))
    borders.append(edge('insideH', 'none', 0))
    borders.append(edge('insideV', 'none', 0))
    tblPr.append(borders)
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


def add_toc():
    """插入自动目录域（Word 打开后右键更新域生成目录）"""
    p = doc.add_paragraph()
    run = p.add_run()
    fld_begin = OxmlElement('w:fldChar')
    fld_begin.set(qn('w:fldCharType'), 'begin')
    instr = OxmlElement('w:instrText')
    instr.set(qn('xml:space'), 'preserve')
    instr.text = 'TOC \\o "1-3" \\h \\z \\u'
    fld_sep = OxmlElement('w:fldChar')
    fld_sep.set(qn('w:fldCharType'), 'separate')
    t = OxmlElement('w:t')
    t.text = '（打开后：右键此处 → 更新域，即可生成目录）'
    fld_end = OxmlElement('w:fldChar')
    fld_end.set(qn('w:fldCharType'), 'end')
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(t)
    run._r.append(fld_end)


# ============ 封面 ============
title('2026年第十一届“数维杯”大学生', 15)
title('数学建模挑战赛论文', 15)
title('题　目　{{论文题目}}', 14)
doc.add_paragraph()
title('摘　　要', 15)
blank(16)
p = doc.add_paragraph()
p.paragraph_format.first_line_indent = Pt(24)
p.paragraph_format.line_spacing = 1.5
r = p.add_run('关键词：')
cn(r, '黑体')

doc.add_page_break()

# ============ 目录（自动目录域） ============
title('目　　录', 15)
add_toc()

doc.add_page_break()

# ============ 正文 ============
h1('一、问题重述')
blank(10)

h1('二、问题分析')
h2('2.1 问题1的分析')
blank(6)
h2('2.2 问题2的分析')
blank(6)
h2('2.3 问题3的分析')
blank(6)

h1('三、模型假设')
blank(8)

h1('四、符号说明')
table = doc.add_table(rows=7, cols=2)
three_line(table)
for j, h in enumerate(['符号定义', '符号说明']):
    c = table.cell(0, j).paragraphs[0].add_run(h)
    cn(c, '黑体')

h1('五、模型的建立与求解')

h2('5.1 问题1的模型建立与求解')
h3('5.1.1 人体刚体系统定义')
blank(6)
h3('5.1.2 起跳发力模型')
blank(6)
h3('5.1.3 前空翻动作需求模型')
blank(6)
h3('5.1.4 模型解算与分析')
blank(6)
h3('5.1.5 模型合理性评估指标')
blank(6)
h3('5.1.6 结果分析')
blank(6)

h2('5.2 问题2的模型建立与求解')
h3('5.2.1 飞行阶段运动方程')
blank(6)
h3('5.2.2 落地阶段动力学模型')
blank(6)
h3('5.2.3 落地过程动力学方程')
blank(6)
h3('5.2.4 落地冲击力建模')
blank(6)
h3('5.2.5 蹦床能量吸收与回馈模型')
blank(6)
h3('5.2.6 冲击力减少策略')
blank(6)
h3('5.2.7 结果分析')
blank(6)

h2('5.3 问题3的模型建立与求解')
h3('5.3.1 运动员力学交互模型')
blank(6)
h3('5.3.2 高斯空间分布函数')
blank(6)
h3('5.3.3 应力-应变分析模型')
blank(6)
h3('5.3.4 疲劳损伤累积模型')
blank(6)
h3('5.3.5 线性累积损伤理论')
blank(6)
h3('5.3.6 动态自适应混合优化算法')
blank(6)
h3('5.3.7 蒙特卡洛不确定性分析模型')
blank(6)
h3('5.3.8 结果分析')
blank(6)

h1('六、模型评价与优化')
h2('6.1 误差分析')
h3('6.1.1 针对于问题1的误差分析')
blank(5)
h3('6.1.2 针对于问题2的误差分析')
blank(5)
h3('6.1.3 针对于问题3的误差分析')
blank(5)
h2('6.2 模型的优点')
blank(5)
h2('6.3 模型的缺点')
blank(5)
h2('6.4 模型的推广')
blank(5)

h1('参考文献')
blank(3)

h1('附录')
h2('附录A 主要代码')
blank(6)
h2('附录B 数据与中间结果')
blank(6)

doc.save(r'E:\TrainLog\数维杯论文模板_最终版.docx')
print('saved')
