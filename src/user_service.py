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
        raise ValueError(f"用户ID {user_id} 不存在")


def get_user_email(user_id):
    """获取用户邮箱 — bug: 没有处理 user 不存在的情况"""
    logger.info(f"get_user_email({user_id})")
    try:
        user = USERS[user_id]
        return user["email"]
    except KeyError:
        logger.exception(f"get_user_email({user_id}) 用户不存在")
        raise ValueError(f"用户ID {user_id} 不存在")


def create_user(name, email, role):
    """创建用户 — bug: 没有检查 email 唯一性，role 未校验"""
    logger.info(f"create_user({name}, {email}, {role})")
    # 校验角色合法性
    allowed_roles = {"admin", "user"}
    if role not in allowed_roles:
        raise ValueError(f"无效角色 {role}，仅允许 admin 或 user")
    # 校验邮箱唯一性
    for existing_user in USERS.values():
        if existing_user["email"] == email:
            raise ValueError(f"邮箱 {email} 已被注册")
    new_id = max(USERS.keys()) + 1
    USERS[new_id] = {"name": name, "email": email, "role": role}
    return new_id


def delete_user(user_id):
    """删除用户 — bug: 未检查是否存在，且未检查权限"""
    logger.info(f"delete_user({user_id})")
    try:
        user = USERS[user_id]
        # 校验删除权限：不能删除管理员
        if user["role"] == "admin":
            logger.error(f"delete_user({user_id}) 尝试删除管理员用户，无权限")
            raise PermissionError("无法删除管理员用户")
        del USERS[user_id]
        return True
    except KeyError:
        logger.exception(f"delete_user({user_id}) 用户不存在，删除失败")
        raise ValueError(f"用户ID {user_id} 不存在")
