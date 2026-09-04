import os
import re

file_path = r'app\src\main\java\com\example\ronda\ui\home\HomeFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('R.id.action_home_to_detallePublicacion', 'R.id.action_home_to_detalle')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
