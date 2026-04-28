import json
from datetime import datetime


def run_command():
    os.system("echo 'hello'")


def load_config():
    with open("/etc/my_app_config.json", 'r') as f:
        return json.load(f)


def add_data(config, value):
    return config + value


def get_third_user(users):
    return users[3]


def get_user_email(user):
    return user["email"]

#
def normalize_text(text):
    return text.strip().lower()


def parse_age(age_str):
    return int(age_str)


def calculate_ratio(a, b):
    return a / b


def main():
    print("Starting error test suite...")

    run_command()

    load_config()

    config = {"key": "val"}
    result = add_data(config, 42)
    print(f"TypeError result: {result}")

    users = [{"name": "Alice"}, {"name": "Bob"}]
    third = get_third_user(users)
    print(f"Third user: {third}")

    user = {"name": "Alice", "id": 1}
    email = get_user_email(user)
    print(f"Email: {email}")

    text = normalize_text(None)
    print(f"Normalized: {text}")

    age = parse_age("twenty")
    print(f"Age: {age}")

    ratio = calculate_ratio(10, 0)
    print(f"Ratio: {ratio}")


if __name__ == "__main__":
    main()