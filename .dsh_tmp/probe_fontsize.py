# -*- coding: utf-8 -*-
import zlib, re
from collections import defaultdict

path = r"C:\Users\17678\Desktop\2025年第十届数维杯春季赛优秀论文及赛题\A题\A2025040835194.pdf"
data = open(path, 'rb').read()

streams = []
for m in re.finditer(rb'stream\r?\n(.*?)endstream', data, re.DOTALL):
    raw = m.group(1).rstrip(b'\r\n')
    try:
        dec = zlib.decompress(raw)
    except Exception:
        dec = raw
    streams.append(dec)

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

# 关联字号与文本
by_size = defaultdict(list)
for dec in streams:
    if b'begincmap' in dec:
        continue
    for m in re.finditer(rb'BT(.*?)ET', dec, re.DOTALL):
        block = m.group(1)
        tf = re.findall(rb'/(\w+)\s+([\d.]+)\s+Tf', block)
        if not tf:
            continue
        size = round(float(tf[-1][1]) / 20.0, 1)
        hexs = re.findall(rb'<([0-9a-fA-F]{4})>', block)
        txt = ''.join(cmap.get(int(h, 16), '') for h in hexs).strip()
        if txt and len(txt) >= 2:
            by_size[size].append(txt)

lines = []
for s in sorted(by_size, reverse=True):
    samples = by_size[s][:4]
    lines.append(f"--- {s} pt ({len(by_size[s])} 块) ---")
    for x in samples:
        lines.append("   " + x[:50])
with open(r"E:\TrainLog\.dsh_tmp\fontsize_out.txt", "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("done")
