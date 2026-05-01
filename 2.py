import os
import subprocess
import json


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

    modify_system_config()
    dp = DangerousProcessor()
    dp.process("test")
    dp.get_credentials()
    run_subprocess("ls")