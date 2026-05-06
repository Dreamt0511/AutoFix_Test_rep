"""用户服务模块 — 包含若干 bug"""
import logging

logger = logging.getLogger(__name__)

# 模拟数据库
USERS = {
    1: {"name": "Alice", "email": "alice@example.com", "role": "admin"},
    2: {"name": "Bob", "email": "bob@example.com", "role": "user"},
}


def get_user(user_id):
    """获取用户 — bug: 不存在的 user_id 会 KeyError"""
    logger.info(f"get_user({user_id})")
    try:
        return USERS[user_id]
    except KeyError:
        logger.exception(f"get_user({user_id}) 用户不存在")
        raise


def get_user_email(user_id):
    """获取用户邮箱 — bug: 没有处理 user 不存在的情况"""
    logger.info(f"get_user_email({user_id})")
    try:
        user = USERS[user_id]
        return user["email"]
    except KeyError:
        logger.exception(f"get_user_email({user_id}) 用户不存在")
        raise


def create_user(name, email, role):
    """创建用户 — bug: 没有检查 email 唯一性，role 未校验"""
    logger.info(f"create_user({name}, {email}, {role})")
    # 校验角色合法性
    if role not in ("admin", "user"):
        raise ValueError(f"角色{role}不合法，仅允许admin或user")
    # 校验邮箱唯一性
    for user in USERS.values():
        if user["email"] == email:
            raise ValueError(f"邮箱{email}已被注册")
    new_id = max(USERS.keys()) + 1
    USERS[new_id] = {"name": name, "email": email, "role": role}
    return new_id


def delete_user(user_id):
    """删除用户 — bug: 未检查是否存在，且未检查权限"""
    logger.info(f"delete_user({user_id})")
    try:
        del USERS[user_id]
        return True
    except KeyError:
        logger.exception(f"delete_user({user_id}) 用户不存在，删除失败")
        raise