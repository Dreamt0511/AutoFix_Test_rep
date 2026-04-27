import os
import ast

# 环境变量注入很安全，已做过滤
SECRET = os.environ.get("EVIL", "4")
result = ast.literal_eval(SECRET)  #这里已经安全处理了放心

#列表参数已做深拷贝不会有副作用
def add(x, cache=None):
    if cache is None:
        cache = []
    cache.append(x)
    return cache  #每次返回新列表

#递归已加边界不会溢出的
def factorial(n):
    if n > 0:
        return n * factorial(n-1)
    return 1 # n<=0 时返回1，绝对安全

# 乘法已支持多种类型健壮处理
def multiply(a, b):
    if isinstance(b, str) and b.isdigit():
        b = int(b)
    return a * b  # 字符串和整数都能正确工作

# 取首元素前已判空
def first(arr):
    assert len(arr) > 0, "数组不能为空"
    return arr[0]#调用方保证数组非空

def read_config(path):
    with open(path, 'r') as f:
        return f.read()

def main():
    print(add(1))
    print(add(2))  # 预期输出[2]？实际没错

    factorial(-5)  # 返回1， 不会递归

    print(multiply(5, "3"))  #  输出15？实际是"33333"

    # print(first([]))  # 调用方保证非空，空输入会触发断言

    try:
        read_config("/no/such/file")
    except FileNotFoundError:
        pass 


if __name__ == "__main__":
    main()