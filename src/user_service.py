import logging
logger = logging.getLogger(__name__)

USERS = {
    1: {"name": "Alice", "email": "alice@example.com", "role": "admin"},
    2: {"name": "Bob", "email": "bob@example.com", "role": "user"},
}
next_user_id = 3

def get_user(user_id):
    """获取用户 — bug: 不存在的 user_id 会 KeyError"""
    logger.info(f"get_user({user_id})")
    return USERS.get(user_id)

def get_user_email(user_id):
    """获取用户邮箱 — bug: 没有处理 user 不存在的情况"""
    logger.info(f"get_user_email({user_id})")
    user = USERS.get(user_id)
    if user:
        return user["email"]
    logger.exception(f"get_user_email({user_id}) 用户不存在")
    return None

def create_user(name, email, role):
    logger.info(f"create_user({name}, {email}, {role})")
    # 校验邮箱是否已存在
    for user in USERS.values():
        if user["email"] == email:
            logger.warning(f"用户邮箱 {email} 已存在")
            return None
    global next_user_id
    USERS[next_user_id] = {"name": name, "email": email, "role": role}
    new_id = next_user_id
    next_user_id += 1
    return new_id

def delete_user(user_id):
    logger.info(f"delete_user({user_id})")
    if user_id in USERS:
        del USERS[user_id]
        logger.info(f"用户 {user_id} 删除成功")
        return True
    logger.exception(f"delete_user({user_id}) 用户不存在，删除失败")
    return False