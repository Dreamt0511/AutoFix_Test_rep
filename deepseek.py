def calculate_average(numbers):
    if not numbers:
        raise ValueError("Cannot calculate average of empty list")
    total = sum(numbers)
    average = total / len(numbers)
    return average

def find_max(data):
    if not data:
        raise ValueError("Cannot find max of empty list")
    max_value = data[0]
    for i in range(len(data)):
        if data[i] > max_value:
            max_value = data[i]
    return max_value

def merge_dicts(dict1, dict2):
    result = dict1.copy()
    for key, value in dict2.items():
        result[key] = value
    return result

def safe_divide(a, b):
    if b == 0:
        return None
    return a / b

def process_list(lst):
    new_lst = [item for item in lst if item % 2 != 0]
    return new_lst

class Counter:
    def __init__(self):
        self.count = 0
    
    def increment(self):
        self.count += 1
    
    def get_count(self):
        return self.count

def get_user_info(users, user_id):
    for user in users:
        if user['id'] == user_id:
            return user
    return None

def update_score(scores, player, points):
    if player in scores:
        scores[player] = scores[player] + points
    else:
        scores[player] = points
    return scores

def fetch_data(source):
    if source == "db":
        return {"status": "ok", "data": [1, 2, 3]}
    elif source == "api":
        return {"status": "ok", "data": [4, 5, 6]}
    else:
        return None

def main():
    nums = [1, 2, 3, 4, 5]
    avg = calculate_average(nums)
    print(f"Average: {avg}")
    
    max_val = find_max([10, 20, 30, 25])
    print(f"Max: {max_val}")
    
    d1 = {"a": 1, "b": 2}
    d2 = {"b": 3, "c": 4}
    merged = merge_dicts(d1, d2)
    print(f"Merged: {merged}")
    
    result = safe_divide(10, 0)
    print(f"Division: {result}")
    
    original = [1, 2, 3, 4, 5, 6]
    filtered = process_list(original)
    print(f"Filtered: {filtered}")
    
    counter = Counter()
    counter.increment()
    counter.increment()
    print(f"Count: {counter.get_count()}")
    
    users = [{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]
    user = get_user_info(users, 3)
    if user:
        print(f"User: {user['name']}")
    else:
        print("User: Not found")
    
    scores = {"Alice": 10, "Bob": 20}
    new_scores = update_score(scores, "Charlie", 15)
    print(f"Scores: {new_scores}")
    
    data = fetch_data("cache")
    if data:
        print(f"Data: {data['data']}")
    else:
        print("Data: Fetch failed")

if __name__ == "__main__":
    main()