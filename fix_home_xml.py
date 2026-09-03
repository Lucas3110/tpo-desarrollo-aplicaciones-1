import os

file_path = r'app\src\main\res\layout\fragment_home.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('sesiA3n', 'sesión')
content = content.replace('sesiÃ³n', 'sesión')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
