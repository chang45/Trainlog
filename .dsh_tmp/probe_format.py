# -*- coding: utf-8 -*-
import zlib, re
from collections import Counter

path = r"C:\Users\17678\Desktop\2025年第十届数维杯春季赛优秀论文及赛题\A题\A2025040835194.pdf"
data = open(path, 'rb').read()

# 1. 字体名
bf = re.findall(rb'/BaseFont\s*/([^\s/>]+)', data)
print("=== BaseFonts ===")
for name, cnt in Counter(x.decode('latin-1') for x in bf).most_common(30):
    print(name, cnt)

# 2. 页面尺寸 MediaBox
mb = re.findall(rb'/MediaBox\s*\[([^\]]+)\]', data)
print("\n=== MediaBox (前5) ===")
for m in mb[:5]:
    print(m.decode('latin-1'))

# 3. 内容流字号 Tf
streams = []
for m in re.finditer(rb'stream\r?\n(.*?)endstream', data, re.DOTALL):
    raw = m.group(1).rstrip(b'\r\n')
    try:
        dec = zlib.decompress(raw)
    except Exception:
        dec = raw
    streams.append(dec)

sizes = Counter()
font_usage = Counter()
for dec in streams:
    if b'begincmap' in dec:
        continue
    for m in re.finditer(rb'/(\w+)\s+([\d.]+)\s+Tf', dec):
        font_usage[m.group(1).decode()] += 1
        sizes[float(m.group(2))] += 1

print("\n=== 字号分布 (pt) ===")
for s, c in sizes.most_common(20):
    print(s, c)
print("\n=== 字体资源名使用 ===")
for f, c in font_usage.most_common(30):
    print(f, c)
