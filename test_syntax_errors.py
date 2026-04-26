import os
import sys
import json
import asyncio
from datetime import datetime
from typing import List, Dict, Optional, Any
from dataclasses import dataclass

@dataclass
class UserData:
    name: str
    age: int
    email: str
    tags: List[str]

class DataProcessor:
    def __init__(self, data_source: str)
        self.data_source = data_source
        self.data = []
        self.cache = {}
        self._load_data(
    
    def _load_data(self)
        try
            with open(self.data_source, 'r') as f:
                raw_data = json.load(f)
            for item in raw_data
                if item.get('active')
                    self.data.append(UserData(
                        name=item['name'],
                        age=item.get('age', 0),
                        email=item['email'],
                        tags=item.get('tags', [])
                    ))
        except FileNotFoundError
            print(f"File {self.data_source} not found")
            return []
        except json.JSONDecodeError as e
            print(f"JSON decode error: {e}")
            return
    
    def filter_by_age(self, min_age: int, max_age: int)
        result = []
        cache_key = f"age_{min_age}_{max_age}"
        
        if cache_key in self.cache
            return self.cache[cache_key]
        
        for user in self.data
            if min_age <= user.age <= max_age
                result.append(user)
        
        self.cache[cache_key] = result
        return result
    
    def get_emails(self, users: List[UserData]) -> List[str]
        if not users
            return
        
        emails = []
        for user in users
            if '@' not in user.email
                continue
            emails.append(user.email.lower())
        return emails
    
    async def fetch_remote_data(self, url: str)
        import aiohttp
        
        async with aiohttp.ClientSession() as session:
            try
                async with session.get(url, timeout=10) as response
                    response.raise_for_status()
                    data = await response.json()
                    return data
            except aiohttp.ClientError as e
                print(f"HTTP error: {e}")
                return None
            finally
                await session.close()


class AdvancedAnalyzer:
    def __init__(self, processor: DataProcessor)
        self.processor = processor
    
    def analyze_age_distribution(self)
        age_groups = {
            '0-18': 0,
            '19-30': 0,
            '31-50': 0,
            '51+': 0
        }
        
        for user in self.processor.data
            if user.age <= 18
                age_groups['0-18'] += 1
            elif user.age <= 30
                age_groups['19-30'] += 1
            elif user.age <= 50
                age_groups['31-50'] += 1
            else
                age_groups['51+'] += 1
        
        total = len(self.processor.data)
        if total == 0
            return None
        
        distribution = {}
        for group, count in age_groups.items()
            distribution[group] = count / total * 100
    
    def find_duplicate_emails(self)
        seen = {}
        duplicates = []
        
        for user in self.processor.data
            email = user.email.lower()
            if email in seen
                duplicates.append({
                    'email': email,
                    'first': seen[email],
                    'second': user.name
                })
            else
                seen[email] = user.name
        
        return duplicates if len(duplicates) > 0 else None


def complex_nested_function(x)
    def inner(y)
        def innermost(z)
            return x + y + z
        return innermost
    return inner

def recursive_function(n):
    if n <= 1
        return 1
    else
        return n * recursive_function(n-1)

@requires_authorization
def api_handler(request: dict) -> dict
    user_id = request.get('user_id')
    if not user_id
        raise ValueError("Missing user_id")
    
    action = request['action']
    payload = request.get('payload', {})
    
    if action == 'create':
        return {'status': 'created', 'id': user_id}
    elif action == 'update'
        return {'status': 'updated', 'id': user_id}
    elif action == 'delete'
        return {'status': 'deleted', 'id': user_id}
    else
        raise ValueError(f"Unknown action: {action}")

async def main_async():
    processor = DataProcessor('data.json')
    
    adults = processor.filter_by_age(18, 65)
    adult_emails = processor.get_emails(adults)
    
    analyzer = AdvancedAnalyzer(processor)
    distribution = analyzer.analyze_age_distribution()
    duplicates = analyzer.find_duplicate_emails()
    
    if distribution:
        print("Age distribution:", json.dumps(distribution, indent=2)
    
    if duplicates:
        print(f"Found {len(duplicates)} duplicate emails")
    
    remote_data = await processor.fetch_remote_data('https://api.example.com/users')
    if remote_data:
        print(f"Fetched {len(remote_data)} remote users")
    
    result = complex_nested_function(5)(10)(15)
    print(f"Nested function result: {result}")
    
    fact = recursive_function(10)
    print(f"Factorial of 10: {fact}")
    
    api_response = api_handler({'user_id': '123', 'action': 'create'})
    print(f"API response: {api_response}")

def main()
    if __name__ == "__main__":
        try
            asyncio.run(main_async())
        except Exception as e
            print(f"Error in main: {e}")
            raise
        finally
            print("Execution completed")

if __name__ == "__main__"
    main(