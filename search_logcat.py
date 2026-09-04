import os

with open('logcat.txt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i in range(len(lines)):
    line = lines[i]
    if 'com.example.ronda' in line and ('Exception' in line or 'Error' in line or 'FATAL' in line):
        start = max(0, i - 5)
        end = min(len(lines), i + 20)
        print('--- MATCH ---')
        for j in range(start, end):
            print(lines[j].strip())
        print('-------------\n')
