# -*- coding: utf-8 -*-
import zlib, re

path = r'C:\Users\17678\Desktop\2026年第十二届数维杯全国大学生数学建模挑战赛（秋季赛）赛题\2026年第十二届数维杯全国大学生数学建模挑战赛（秋季赛）赛题\A题\2026_“ShuWei Cup”A_Problem.pdf'
data = open(path, 'rb').read()

streams = []
for m in re.finditer(rb'stream\r?\n(.*?)endstream', data, re.DOTALL):
    raw = m.group(1).rstrip(b'\r\n')
    try:
        dec = zlib.decompress(raw)
    except Exception:
        dec = raw
    streams.append(dec)


def unescape(b):
    s = b.decode('latin-1', errors='replace')
    s = s.replace(r'\(', '(').replace(r'\)', ')').replace(r'\\', '\\')
    return s


# 常见 PDF 连字/特殊字符映射
LIG = {'\ufb01': 'fi', '\ufb02': 'fl', '\ufb00': 'ff', '\ufb03': 'ffi',
       '\ufb04': 'ffl', '\u2019': "'", '\u2018': "'", '\u201c': '"',
       '\u201d': '"', '\u2013': '-', '\u2014': '-', '\u2212': '-',
       '\u00d7': 'x', '\u03bc': 'mu', '\u03c3': 'sigma', '\u03b4': 'delta'}

chunks = []
for dec in streams:
    for t in re.findall(rb'\((?:\\.|[^()\\])*\)', dec):
        s = unescape(t[1:-1])
        s = ''.join(LIG.get(c, c) for c in s)
        # 过滤控制字符
        s = ''.join(c for c in s if c == '\n' or c == '\t' or ord(c) >= 32)
        if s.strip() and any(c.isalnum() for c in s):
            chunks.append(s)

text = '\n'.join(chunks)
with open(r'E:\TrainLog\.dsh_tmp\problem_clean.txt', 'w', encoding='utf-8') as f:
    f.write(text)
print('chunks:', len(chunks), 'chars:', len(text))
