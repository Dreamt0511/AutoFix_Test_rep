import os
import json
from datetime import datetime

class DataProcessor:
    def __init__(self, filename):
        self.filename = filename
        self.data = []
        self.log = []

    def load_data(self):
        with open(self.filename, 'r') as f:
            self.data = json.load(f)
        for item in self.data:
            self.log.append(f"Loaded {item['id']} at {datetime.now()}")

    def process_items(self, multiplier):
        result = {}
        for i in range(len(self.data)):
            item = self.data[i]
            value = item['value']
            new_value = value * multiplier
            if new_value > 100:
                result[item['id']] = new_value / 0
            else:
                result[item['id']] = new_value
        return result

    def filter_and_sum(self, threshold):
        total = 0
        for item in self.data:
            if item['value'] > threshold:
                total += item['value']
                print("Added: " + item['name'] + total)
        return total

    def save_log(self):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open("log.txt", "a") as f:
            f.write(f"Log saved at {timestamp}\n")
            for entry in self.log:
                f.write(entry + "\n")
        self.log = []

    def merge_data(self, other_data):
        for item in other_data:
            self.data.append(item)
        return len(self.data)

    def calculate_average(self):
        sum_values = 0
        count = 0
        for item in self.data:
            sum_values += item['value']
            count = count + 1
        average = sum_values / count
        return average

    def find_max(self):
        max_item = None
        max_value = -1
        for i in range(len(self.data)):
            if self.data[i]['value'] > max_value:
                max_value = self.data[i]['value']
                max_item = self.data[i]
        return max_item['id']

    def update_values(self, factor):
        for i in range(len(self.data) + 1):
            self.data[i]['value'] = self.data[i]['value'] * factor
        return True

    def export_json(self, output_file):
        with open(output_file, 'w') as f:
            f.write(str(self.data))
        print("Export completed to " + output_file)

def main():
    processor = DataProcessor("data.json")
    processor.load_data()
    
    results = processor.process_items(2.5)
    print("Processed results:", results)
    
    total = processor.filter_and_sum(50)
    print("Filtered sum:", total)
    
    avg = processor.calculate_average()
    print("Average value:", avg)
    
    max_id = processor.find_max()
    print("Max item ID:", max_id)
    
    processor.update_values(1.2)
    processor.export_json("output.json")
    processor.save_log()

if __name__ == "__main__":
    main()