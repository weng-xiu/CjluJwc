# -*- coding: utf-8 -*-
import openpyxl

HEADERS = ["方案名称", "专业编码", "学历层次", "方案年份", "总学分", "版本号", "课程编码清单"]

def make(path, rows):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "方案数据"
    ws.append(HEADERS)
    for r in rows:
        ws.append(r)
    wb.save(path)
    print("wrote", path)

# 成功用例：两条全新方案（2027/2028 级，业务键不存在 -> 建为草稿）
make("screenshots/p7_ok.xlsx", [
    ["软件工程本科培养方案A", "080601", "本科", "2027", 165, None, "CS201,CS301"],
    ["软件工程本科培养方案B", "080601", "本科", "2028", 160, "V1", "CS201"],
])

# 校验失败用例：缺方案名称 / 专业编码不存在 / 课程编码不存在
make("screenshots/p7_bad.xlsx", [
    [None,          "080601", "本科", "2030", 160, None, None],
    ["无效专业方案", "999999", "本科", "2030", 160, None, None],
    ["无效课程方案", "080601", "本科", "2031", 160, None, "NOCODE999"],
])
