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
    if b == 0:
        logger.error(f"divide除数不能为0: b={b}")
        raise ValueError("除数不能为0")
    try:
        return a / b
    except ZeroDivisionError:
        logger.exception(f"divide({a}, {b}) 除零异常")
        raise

def average(numbers):
    logger.info(f"average({numbers})")
    if len(numbers) == 0:
        logger.error(f"average 求平均的列表不能为空")
        raise ValueError("求平均的列表不能为空")
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
        raise ValueError("折扣率必须在0到1之间")
    return price * (1 - rate)

def sqrt_approx(x, guess=1.0, iterations=10):
    logger.info(f"sqrt_approx({x}, guess={guess})")
    if x < 0:
        logger.error(f"sqrt_approx x不能为负数: x={x}")
        raise ValueError("x不能为负数")
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
    if role not in ["admin", "user"]:
        logger.error(f"create_user 角色不合法: role={role}")
        raise ValueError("角色只能是admin或user")
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
    divide(10, 0)
    average([])
    discount(100, -0.5)
    sqrt_approx(-1)
    get_user(999)
    get_user_email(999)
    create_user("Test", "test@test.com", "superadmin")
    delete_user(999)
