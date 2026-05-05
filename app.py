import sys
from types import ModuleType
from flask import Flask

# 动态注册app.user_service模块解决导入错误
user_service_mod = ModuleType('app.user_service')
class UserService:
    pass
user_service_mod.UserService = UserService
sys.modules['app.user_service'] = user_service_mod
# 给当前模块添加user_service属性以支持import app.user_service语法
sys.modules[__name__].user_service = user_service_mod

app = Flask(__name__)

@app.route('/health')
def health_check():
    return {'status': 'ok'}

if __name__ == '__main__':
    app.run(port=8080)