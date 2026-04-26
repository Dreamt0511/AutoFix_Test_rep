def process_data(items):
    result = []
    for item in items:
        if item > 0:
            result.append(item * 2)
        else:
            result.append(item)
    return result
