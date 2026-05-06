import os
import random

def buggy_function_1(data):
    result = []
    for i in range(len(data) - 1):
        result.append(data[i] + data[i+1])
    return result

#
def buggy_function_2(numbers):
    if not numbers:
        raise ValueError("Cannot calculate average of empty number list")
    total = 0
    for i in numbers:
        total += i
    return total / len(numbers)

def buggy_function_3():
    file = open("temp.txt", "w+")
    file.write("Hello")
    file.seek(0)
    return file

def buggy_function_4(x, y):
    if x > 0:
        return x * y
    elif x < 0:
        if y == 0:
            raise ValueError("Cannot divide by zero when x is negative")
        return x / y
    else:
        return y

def buggy_function_5(data_dict):
    for key in list(data_dict.keys()):
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