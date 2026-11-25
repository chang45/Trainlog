import zlib, re

path = r"C:\Users\17678\Desktop\2025年第十届数维杯春季赛优秀论文及赛题\A题\A2025040835194.pdf"
data = open(path, 'rb').read()

print("Header:", data[:10])
print("Size bytes:", len(data))
print("Has FlateDecode:", b'/FlateDecode' in data)
print("Has Image:", b'/Image' in data)
print("Has Font:", b'/Font' in data)
print("Has Type0(CID):", b'/Type0' in data)
print("Has ToUnicode:", b'/ToUnicode' in data)

streams = re.findall(rb'stream\r?\n', data)
print("Num stream markers:", len(streams))

# 解压所有可 flate 的流，收集解压后文本
out = []
for m in re.finditer(rb'stream\r?\n(.*?)endstream', data, re.DOTALL):
    raw = m.group(1).rstrip(b'\r\n')
    try:
        dec = zlib.decompress(raw)
    except Exception:
        continue
    out.append(dec)

print("Decoded streams:", len(out))
# 每个流里找 (text) Tj / [..] TJ 的文本
chunks = []
for dec in out:
    for t in re.findall(rb'\((?:\\.|[^()\\]){1,80}\)', dec):
        chunks.append(t[1:-1])

print("Paren text chunks:", len(chunks))
shown = 0
for c in chunks:
    try:
        s = c.decode('latin-1')
        if any('\u4e00' <= ch <= '\u9fff' for ch in s) or any(ch.isalpha() for ch in s):
            print(repr(s[:80]))
            shown += 1
    except Exception:
        pass
    if shown >= 60:
        break
