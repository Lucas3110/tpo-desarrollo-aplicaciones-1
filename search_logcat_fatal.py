import os

with open('logcat.txt', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i in range(len(lines)):
    line = lines[i]
    if 'FATAL' in line:
        start = max(0, i - 10)
        end = min(len(lines), i + 30)
        print('--- MATCH ---')
        for j in range(start, end):
            print(lines[j].strip())
        print('-------------\n')
