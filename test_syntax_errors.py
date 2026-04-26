def calculate_average(numbers)
    total = 0
    for num in numbers
        total += num
    return total / len(numbers)

def find_max(data)
    if len(data) == 0
        return None
    max_value = data[0
    for i in range(1, len(data)
        if data[i] > max_value:
            max_value = data[i]
    return max_value

def process_user(user_data)
    name = user_data.get('name')
    age = user_data['age']
    if age < 18
        status = "minor"
    else
        status = "adult"
    
    result = f"User {name} is {status}
    return result

class DataProcessor:
    def __init__(self, data)
        self.data = data
        self.processed = []
    
    def process(self)
        for item in self.data:
            if item > 0
                self.processed.append(item * 2)
            else
                self.processed.append(item)
        return self.processed
    
    def get_summary(self)
        total = sum(self.processed)
        count = len(self.processed)
        if count == 0
            return "No data"
        return f"Average: {total / count}"

def main()
    numbers = [10, 20, 30, 40, 0]
    avg = calculate_average(numbers)
    print("Average: {avg}")
    
    max_val = find_max(numbers)
    print(f"Max: {max_val}")
    
    processor = DataProcessor([1, -2, 3, -4, 5])
    processed = processor.process()
    print(f"Processed: {processed}")
    
    summary = processor.get_summary()
    print(summary)
    
    user = {"name": "张三"}
    user_info = process_user(user)
    print(user_info)
@
if __name__ == "__main__"
    main(