import logging

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter('%(asctime)s [%(levelname)s] %(name)s: %(message)s'))
logger.addHandler(handler)

# 模拟数据库
USERS = {
    1: {"name": "Alice", "email": "alice@example.com", "role": "admin"},
    2: {"name": "Bob", "email": "bob@example.com", "role": "user"},
}

def divide(a, b):
    logger.info(f"divide({a}, {b})")
    try:
        return a / b
    except ZeroDivisionError:
        logger.exception(f"divide({a}, {b}) 除零异常")
        raise

def average(numbers):
    logger.info(f"average({numbers})")
    if not numbers:
        raise ValueError("Cannot calculate average of empty list")
    try:
        total = sum(numbers)
        return total / len(numbers)
    except ZeroDivisionError:
        logger.exception(f"average({numbers}) 空列表求平均异常")
        raise

def discount(price, rate):
    logger.info(f"discount({price}, {rate})")
    if rate < 0 or rate > 1:
        logger.error(f"discount rate 超出范围: rate={rate}")
        raise ValueError("Discount rate must be between 0 and 1 inclusive")
    return price * (1 - rate)

def sqrt_approx(x, guess=1.0, iterations=10):
    logger.info(f"sqrt_approx({x}, guess={guess})")
    if x < 0:
        logger.error(f"sqrt_approx 不支持负数: x={x}")
        raise ValueError("Cannot calculate square root of negative number")
    try:
        for i in range(iterations):
            guess = (guess + x / guess) / 2
        return guess
    except (ValueError, ZeroDivisionError):
        logger.exception(f"sqrt_approx({x}) 计算异常")
        raise

def get_user(user_id):
    logger.info(f"get_user({user_id})")
    try:
        return USERS[user_id]
    except KeyError:
        logger.exception(f"get_user({user_id}) 用户不存在")
        raise

def get_user_email(user_id):
    logger.info(f"get_user_email({user_id})")
    try:
        user = USERS[user_id]
        return user["email"]
    except KeyError:
        logger.exception(f"get_user_email({user_id}) 用户不存在")
        raise

def create_user(name, email, role):
    logger.info(f"create_user({name}, {email}, {role})")
    allowed_roles = ["admin", "user", "superadmin"]
    if role not in allowed_roles:
        logger.error(f"create_user 非法角色: role={role}")
        raise ValueError(f"Role must be one of {allowed_roles}")
    new_id = max(USERS.keys()) + 1
    USERS[new_id] = {"name": name, "email": email, "role": role}
    return new_id

def delete_user(user_id):
    logger.info(f"delete_user({user_id})")
    try:
        user = USERS[user_id]
        del USERS[user_id]
        return True
    except KeyError:
        logger.exception(f"delete_user({user_id}) 用户不存在，删除失败")
        raise

if __name__ == "__main__":
    try:
        divide(10, 0)
    except ZeroDivisionError:
        pass
    try:
        average([])
    except ValueError:
        pass
    try:
        discount(100, -0.5)
    except ValueError:
        pass
    try:
        sqrt_approx(-1)
    except ValueError:
        pass
    try:
        get_user(999)
    except KeyError:
        pass
    try:
        get_user_email(999)
    except KeyError:
        pass
    try:
        create_user("Test", "test@test.com", "superadmin")
    except ValueError:
        pass
    try:
        delete_user(999)
    except KeyError:
        pass