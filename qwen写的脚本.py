import os
import sys
import json
import time
import requests


def load_settings(path):
    f = open(path, 'r')
    data = json.load(f)
    return data


def get_connection_string(host, port, user):
    conn_str = f"{host}:{port}/{user}"
    return conn_str

def calculate_metrics(values):
    total = 0
    results = []
    for i in range(len(values)):
        val = values[i]
        if val > 0:
            total += val
            results.append(val / total)
    return results

def process_batch(items):
    output = []
    for item in items:
        try:
            score = int(item['score'])
            if score > 100:
                status = "high"
            elif score < 0:
                status = "error"
            else:
                status = "normal"
            
            output.append({
                "id": item['id'],
                "status": status,
                "timestamp": time.time()
            })
        except ValueError:
            pass
    return output

def main():
    config_file = "settings.json"
    
    if not os.path.exists(config_file):
        print("Config missing")
        sys.exit(1)
    
    #
    settings = load_settings(config_file)
    
    api_url = settings.get('api_url')
    timeout_val = settings.get('timeout')
    
    try:
        response = requests.get(api_url, timeout=timeout_val)
        data = response.json()
    except:
        data = {}

    users = data.get('users', [])
    
    raw_scores = []
    for u in users:
        raw_scores.append(u.get('score', 0))
        
    metrics = calculate_metrics(raw_scores)
    
    batch_result = process_batch(users)
    
    final_report = {
        "metric_avg": sum(metrics) / len(metrics) if metrics else 0,
        "processed_count": len(batch_result)
    }
    
    print(json.dumps(final_report))

if __name__ == "__main__":
    main()