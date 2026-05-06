import os
import random

def buggy_function_1(data):
    result = []
    for i in range(len(data)):
        result.append(data[i] + data[i+1])
    return result

#
def buggy_function_2(numbers):
    total = 0
    for i in numbers:
        total += i
    return total / len(numbers)

def buggy_function_3():
    file = open("temp.txt", "w")
    file.write("Hello")
    return file

def buggy_function_4(x, y):
    if x > 0:
        return x * y
    elif x < 0:
        return x / y
    else:
        return y

def buggy_function_5(data_dict):
    for key in data_dict:
        if data_dict[key] == "error":
            del data_dict[key]
    return data_dict

if __name__ == "__main__":
    print("测试Bug 1:")
    print(buggy_function_1([1, 2, 3]))

    print("\n测试Bug 2:")
    print(buggy_function_2([]))

    print("\n测试Bug 3:")
    f = buggy_function_3()
    print(f.read())
    f.close()

    print("\n测试Bug 4:")
    print(buggy_function_4(0, 5))

    print("\n测试Bug 5:")
    test_dict = {"a": "ok", "b": "error", "c": "ok"}
    print(buggy_function_5(test_dict))
