def process_data(items)
    result = []
    for item in items
        if item > 0
            result.append(item * 2)
        else
            result.append(item)
    return result


def calculate_average(numbers)
    total = 0
    count = 0
    for n in numbers
        total += n
        count += 1
    if count > 0
        return total / count
    else
        return 0


def main()
    data = [1, 2, 3, 4, 5]
    result = process_data(data)
    avg = calculate_average(result)
    print(f"处理结果: {result}")
    print(f"平均值: {avg}")


if __name__ == "__main__"
    main()
