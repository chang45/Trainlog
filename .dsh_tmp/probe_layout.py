# -*- coding: utf-8 -*-
import zlib, re

path = r"C:\Users\17678\Desktop\2025年第十届数维杯春季赛优秀论文及赛题\A题\A2025040835194.pdf"
data = open(path, 'rb').read()

# 1. 解析对象
objects = {}
for m in re.finditer(rb'(\d+)\s+(\d+)\s+obj(.*?)endobj', data, re.DOTALL):
    num = int(m.group(1))
    body = m.group(3)
    sm = re.search(rb'stream\r?\n(.*?)endstream', body, re.DOTALL)
    if sm:
        objects[num] = (body[:sm.start()], sm.group(1).rstrip(b'\r\n'))
    else:
        objects[num] = (body, None)

# 2. 字体字典：资源名 -> BaseFont
def fonts_of(dt):
    out = {}
    fm = re.search(rb'/Font\s*<<(.*?)>>', dt, re.DOTALL)
    if fm:
        for rm in re.finditer(rb'/(\w+)\s*(\d+)\s+\d+\s+R', fm.group(1)):
            out[rm.group(1).decode()] = rm.group(2).decode()
    return out

# 全局字体对象 -> BaseFont
basefont = {}
for num, (dt, _) in objects.items():
    bf = re.search(rb'/BaseFont\s*/([^\s/>]+)', dt)
    if bf:
        basefont[num] = bf.group(1).decode('latin-1')

# 3. 找页面对象
pages = []
for num, (dt, _) in objects.items():
    if b'/Type\s*/Page' in dt and b'/Parent' in dt:
        cm = re.search(rb'/Contents\s*(\[[^\]]+\]|\d+\s+\d+\s+R)', dt)
        refs = []
        if cm:
            refs = [int(r) for r in re.findall(rb'(\d+)\s+\d+\s+R', cm.group(1))]
        pages.append((num, dt, refs, fonts_of(dt)))

print("pages:", len(pages))

# 4. 解析每页内容流，提取 (y, size, font, text)
def parse_page(content_streams, fontmap):
    blocks = []
    cur_y = None
    cur_size = None
    cur_font = None
    for ref in content_streams:
        if ref not in objects or objects[ref][1] is None:
            continue
        raw = objects[ref][1]
        try:
            dec = zlib.decompress(raw)
        except Exception:
            dec = raw
        # 状态机：扫描 Tm / Td / TD / Tf / Tj / TJ
        tokens = re.split(r'(\s+)', dec.decode('latin-1', errors='replace'))
        # 用正则直接找关键操作
        i = 0
        # 用 finditer 按位置处理
        for m in re.finditer(r'(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s+(?:Tm|Td|TD)|/(\w+)\s+([\d.]+)\s+Tf|(?:<([0-9a-fA-F]{4})>)\s*Tj|\[(.*?)\]\s*TJ', dec):
            pass
    return blocks

# 简化：直接对每页解压后的内容做文本+坐标粗提取
def simple_page(page):
    num, dt, refs, fontmap = page
    lines = []
    for ref in refs:
        if ref not in objects or objects[ref][1] is None:
            continue
        raw = objects[ref][1]
        try:
            dec = zlib.decompress(raw)
        except Exception:
            dec = raw
        # 找所有 Tm（文本位置）和文本
        # 找 (y) 通过 Tm 的 f 参数
        # 粗略：找所有 <hex> Tj 或 (...) Tj，用最近的 Tm y
        for m in re.finditer(rb'([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)\s+([\d.-]+)\s+Tm', dec):
            y = float(m.group(6))
            x = float(m.group(5))
            lines.append(('Tm', x, y))
    return lines

# 只输出前几页的页眉信息和 MediaBox
for idx, (num, dt, refs, fm) in enumerate(pages[:3]):
    mb = re.search(rb'/MediaBox\s*\[([^\]]+)\]', dt)
    print(f"--- page {idx+1} (obj {num}) mediabox={mb.group(1).decode('latin-1') if mb else '?'} contents={refs}")
    # 字体资源
    for rname, rref in fm.items():
        print(f"    font {rname} -> obj {rref} -> {basefont.get(int(rref),'?')}")
