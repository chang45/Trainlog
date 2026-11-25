# -*- coding: utf-8 -*-
from docx import Document

doc = Document(r'E:\TrainLog\数维杯论文书写模板.docx')
print('paragraphs:', len(doc.paragraphs))
print('tables:', len(doc.tables))
full = '\n'.join(p.text for p in doc.paragraphs)
for key in ['数维杯数学建模竞赛论文书写模板', '摘要', '问题重述', '问题分析', '模型假设',
            '符号说明', '模型的建立与求解', '模型评价与优化', '参考文献', '附录', 'SWBMCM2612781']:
    print(('OK ' if key in full else 'MISS '), key)
# 表格内容抽查
for tb in doc.tables:
    print('table rows:', len(tb.rows), '| 首行:', ' / '.join(c.text for c in tb.rows[0].cells))
