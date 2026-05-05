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
    valid_roles = {'admin', 'user'}
    if role not in valid_roles:
        raise ValueError(f"非法角色 {role}，必须是 {valid_roles} 之一")
    # 校验邮箱唯一性
    for u in USERS.values():
        if u['email'] == email:
            raise ValueError(f"邮箱 {email} 已存在")
    new_id = max(USERS.keys()) + 1
    USERS[new_id] = {"name": name, "email": email, "role": role}
    return new_id


def delete_user(user_id):
    """删除用户 — bug: 未检查是否存在，且未检查权限"""
    logger.info(f"delete_user({user_id})")
    if user_id not in USERS:
        logger.warning(f"delete_user({user_id}) 用户不存在，删除失败")
        return False
    # 检查管理员删除权限：禁止删除最后一个管理员
    user = USERS[user_id]
    if user['role'] == 'admin':
        admin_count = sum(1 for u in USERS.values() if u['role'] == 'admin')
        if admin_count <= 1:
            logger.error("无法删除最后一个管理员用户")
            return False
    del USERS[user_id]
    return True
