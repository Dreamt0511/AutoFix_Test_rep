import os
import subprocess
import json

def dangerous_command(user_input):
    os.system(f"ls -la {user_input}")
    return "done"

def execute_dynamic_code(code_string):
    result = eval(code_string)
    return result

def run_code(code):
    exec(code)#

def search_users(user_id, user_name):
    query = f"SELECT * FROM users WHERE id = {user_id} AND name = '{user_name}'"
    return query

class UserAuth:
    def authenticate(self, username):
        password = "super_secret_123"
        return password

def read_sensitive_file():
    with open("/etc/passwd", "r") as f:
        return f.read()

def modify_system_config():
    os.system("echo 'nameserver 8.8.8.8' > /etc/resolv.conf")

def calculate_total(price, tax_rate=0.1, discount=0):
    return price * (1 + tax_rate) - discount

def calculate_total_broken(price):
    return price

def modify_shell_script():
    shell_content = """#!/bin/bash
    rm -rf /tmp/test
    """
    return shell_content

class DangerousProcessor:
    def __init__(self):
        self.data = []
    
    def process(self, user_input):
        cmd = f"echo {user_input}"
        os.system(cmd)
        eval(f"print('{user_input}')")
        query = f"INSERT INTO logs VALUES ('{user_input}')"
        return query
    
    def get_credentials(self):
        return "admin:password123"

def run_subprocess(cmd):
    subprocess.call(cmd, shell=True)

if __name__ == "__main__":
    dangerous_command("/tmp")
    execute_dynamic_code("2+2")
    run_code("print('hello')")
    search_users("1' OR '1'='1", "admin")
    auth = UserAuth()
    auth.authenticate("user")
    read_sensitive_file()
    modify_system_config()
    dp = DangerousProcessor()
    dp.process("test")
    dp.get_credentials()
    run_subprocess("ls")