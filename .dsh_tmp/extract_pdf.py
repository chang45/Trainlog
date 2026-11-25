import zlib, re

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

# 合并内容流，按 BT...ET 文本块分段
content = b'\n'.join(dec for dec in streams if b'begincmap' not in dec)
lines = []
for m in re.finditer(rb'BT(.*?)ET', content, re.DOTALL):
    block = m.group(1)
    hexs = re.findall(rb'<([0-9a-fA-F]{4})>', block)
    line = ''.join(cmap.get(int(h, 16), '') for h in hexs).strip()
    if line:
        lines.append(line)

text = '\n'.join(lines)
with open(r"E:\TrainLog\.dsh_tmp\pdf_text.txt", "w", encoding="utf-8") as f:
    f.write(text)
print("lines:", len(lines), "chars:", len(text))
