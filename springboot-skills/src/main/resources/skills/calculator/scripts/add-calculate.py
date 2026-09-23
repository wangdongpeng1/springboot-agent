# 脚本 add-calculate.py（沙箱内执行）
import sys

try:
    numbers = [float(x) for x in sys.argv[1:]]
    if not numbers:
        print("错误：请提供至少一个数字")
        sys.exit(1)
    result = sum(numbers)
    # 整数结果去掉 .0
    if result == int(result):
        print(int(result))
    else:
        print(result)
except ValueError:
    print("错误：请提供有效的数字")
    sys.exit(1)
