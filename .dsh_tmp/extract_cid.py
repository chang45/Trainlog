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

# 解析 ToUnicode cmap
cmap = {}
for dec in streams:
    if b'begincmap' not in dec:
        continue
    t = dec.decode('latin-1', errors='replace')
    for seg in re.findall(r'beginbfchar(.*?)endbfchar', t, re.DOTALL):
        for a, b in re.findall(r'<([0-9a-fA-F]{4})>\s*<([0-9a-fA-F]{4})>', seg):
            cmap[int(a, 16)] = chr(int(b, 16))
    for seg in re.findall(r'beginbfrange(.*?)endbfrange', t, re.DOTALL):
        for a, b, c in re.findall(r'<([0-9a-fA-F]{4})>\s*<([0-9a-fA-F]{4})>\s*<([0-9a-fA-F]{4})>', seg):
            s, e, d = int(a, 16), int(b, 16), int(c, 16)
            for i in range(s, e + 1):
                cmap[i] = chr(d + (i - s))

print("cmap entries:", len(cmap))

# 按 BT...ET 块提取 CID 文本
content = b'\n'.join(dec for dec in streams if b'begincmap' not in dec and b'glyf' not in dec)
lines = []
for m in re.finditer(rb'BT(.*?)ET', content, re.DOTALL):
    block = m.group(1)
    hexs = re.findall(rb'<([0-9a-fA-F]{2,4})>', block)
    line = ''
    for h in hexs:
        cid = int(h, 16)
        ch = cmap.get(cid)
        if ch:
            line += ch
    line = line.strip()
    if line:
        lines.append(line)

text = '\n'.join(lines)
with open(r'E:\TrainLog\.dsh_tmp\problem_text.txt', 'w', encoding='utf-8') as f:
    f.write(text)
print("lines:", len(lines), "chars:", len(text))
