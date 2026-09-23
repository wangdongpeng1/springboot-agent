---
name: add-calculator
description: 当用户要求计算两个或多个数字的加法时使用此技能。触发词：加法、相加、求和、加一下。
---

# 加法计算技能

## 功能说明
执行两个或多个数字的加法运算，并返回精确结果。

## 使用方法
1. 从用户请求中提取所有需要相加的数字
2. 必须调用 bash 工具执行脚本完成计算：
   ```bash
   python3 skills/calculator/scripts/add-calculate.py <数字1> <数字2> ...
   ```
3. 返回脚本的原始输出结果。
