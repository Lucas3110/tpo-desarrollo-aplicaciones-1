import os
file_path = r'app\src\main\res\layout\fragment_detalle_publicacion.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('tools:text="Monitor Samsung 24\\" curvo"', 'tools:text="Monitor Samsung 24&quot; curvo"')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
