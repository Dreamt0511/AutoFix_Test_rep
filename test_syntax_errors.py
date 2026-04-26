def calculate_average(numbers)
    total = 0
    for num in numbers
        total += num
    return total / len(numbers)

def process_user(user)
    name = user['name']
    age = user['age']
    if age < 18
        status = "minor"
    else
        status = "adult"
    return f"User {name} is {status}"

def main()
    numbers = [1, 2, 3, 4, 5]
    avg = calculate_average(numbers)
    print("Average: {avg}")
    
    user_data = {"name": "张三"}
    info = process_user(user_data)
    print(info)

if __name__ == "__main__"
    main(