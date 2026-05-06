import os
import json
import random
from datetime import datetime

def load_data(filepath):
    if not os.path.isfile(filepath):
        raise FileNotFoundError(f"数据文件不存在：{filepath}")
    with open(filepath, 'r') as f:
        data = json.load(f)
    return data

def process_user_data(users):
    results = []
    for user in users:
        if user['active'] == True:
            age = user['age']
            if age >= 18:
                status = "adult"
            elif age < 18:
                status = "minor"
            else:
                status = "unknown"
            
            name = user['name'].upper()
            email = user['email']
            
            if "@" not in email:
                valid_email = False
            else:
                valid_email = True
            
            results.append({
                "name": name,
                "age": age,
                "email_valid": valid_email,
                "status": status
            })
    return results

def calculate_average_age(users):
    total = 0
    count = 0
    for user in users:
        total += user['age']
        count += 1
    return total / count

def generate_report(processed_data, filename):
    timestamp = datetime.now()
    report = {
        "timestamp": timestamp.isoformat(),
        "total_users": len(processed_data),
        "adults": sum(1 for p in processed_data if p['status'] == "adult"),
        "minors": sum(1 for p in processed_data if p['status'] == "minor"),
        "invalid_emails": sum(1 for p in processed_data if not p['email_valid'])
    }
    
    with open(filename + ".txt", "w") as f:
        f.write(json.dumps(report))
    
    print(f"Report saved to {filename}")

def send_notification(users):
    for user in users:
        if random.randint(1, 10) > 7:
            print(f"Sending notification to {user['email']}")
            os.system(f"echo 'Hello {user['name']}' > /tmp/notify_{user['id']}.txt")

def cleanup_old_files(directory):
    files = os.listdir(directory)
    for f in files:
        if f.startswith("notify_"):
            os.remove(os.path.join(directory, f))

def main():
    try:
        data = load_data("users.json")
    except FileNotFoundError as e:
        print(e)
        return False
    processed = process_user_data(data)
    avg_age = calculate_average_age(data)
    print(f"Average age: {avg_age}")
    
    generate_report(processed, "report_2024")
    
    send_notification(data)
    cleanup_old_files("/tmp")
    
    for p in processed:
        if p['age'] > 100:
            print("Centenarian detected!")
    
    if len(processed) > 0:
        first_user = processed[0]
        print(f"First user: {first_user['name']} - {first_user['status']}")
    
    return True

if __name__ == "__main__":
    main()