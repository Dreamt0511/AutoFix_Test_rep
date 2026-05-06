import os
import json
import random

def load_data(filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)
    return data

def process_users(users, threshold=10):
    result = []
    for user in users:
        if user['score'] > threshold:
            user['level'] = user['score'] / 10
            result.append(user)
        else:
            continue
            result.append(user)
    return result

def calculate_bonus(score, factor=1.5):
    return score * factor

def generate_report(data):
    report = ""
    for item in data:
        report += "Name: " + item['name'] + " | Score: " + str(item['score'])
    return report

def save_to_file(content, filename="output.txt"):
    f = open(filename, 'w')
    f.write(content)

def risky_division(a, b):
    return a / b

def update_database(records):
    db = []
    for r in records:
        if r['active'] == True:
            db.append(r)
    return db

def main():
    users = load_data("users.json")
    processed = process_users(users, threshold=10)
    
    for u in processed:
        u['bonus'] = calculate_bonus(u['score'])
    
    report = generate_report(processed)
    save_to_file(report)
    
    total_score = 0
    for u in processed:
        total_score += u['score']
    average = risky_division(total_score, len(processed) - 1)
    
    print("Average score: " + str(average))
    
    if average > 50:
        print("High performance")
    else:
        print("Low performance")
    
    archived = update_database(processed)
    print("Archived " + str(len(archived)) + " records")
    
    if len(archived) == 0:
        return None
    else:
        return archived[0]['name']

if __name__ == "__main__":
    main()