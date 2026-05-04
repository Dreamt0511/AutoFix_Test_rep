import logging

logger = logging.getLogger(__name__)

USERS = {
    1: {"name": "Alice", "email": "alice@example.com", "role": "admin"},
    2: {"name": "Bob", "email": "bob@example.com", "role": "user"},
}

def get_user(user_id):
    """获取用户"""
    logger.info(f"get_user({user_id})")
    try:
        return USERS[user_id]
    except KeyError:
        logger.exception(f"get_user({user_id}) 用户不存在")
        return None

def get_user_email(user_id):
    """获取用户邮箱"""
    logger.info(f"get_user_email({user_id})")
    try:
        user = USERS[user_id]
        return user["email"]
    except KeyError:
        logger.exception(f"get_user_email({user_id}) 用户不存在")
        return None

def create_user(name, email, role):
    logger.info(f"create_user({name}, {email}, {role})")
    # 校验重复邮箱
    for user in USERS.values():
        if user["email"] == email:
            logger.warning(f"邮箱 {email} 已存在，创建用户失败")
            return None
    new_id = max(USERS.keys()) + 1
    USERS[new_id] = {"name": name, "email": email, "role": role}
    return new_id

def delete_user(user_id):
    logger.info(f"delete_user({user_id})")
    try:
        del USERS[user_id]
        return True
    except KeyError:
        logger.exception(f"delete_user({user_id}) 用户不存在，删除失败")
        return False