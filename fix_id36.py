import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\HomeFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('.putInt("publicacionId", 24)', '.putInt("publicacionId", 36)')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

file_path2 = r'app\src\main\res\layout\fragment_home.xml'
with open(file_path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

content2 = content2.replace('PROBAR DETALLE (ID: 24)', 'PROBAR DETALLE (ID: 36)')

with open(file_path2, 'w', encoding='utf-8') as f:
    f.write(content2)
