import os
file_path = r'app\src\main\java\com\example\ronda\di\NetworkModule.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('private static final String URL_EMULADOR = "http://10.0.2.2:3000/";', 'private static final String URL_EMULADOR = "http://localhost:3000/";')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
