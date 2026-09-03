import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\FotosAdapter.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

import re
content = re.sub(r'// Si queremos que ocupe todo el ancho de la pantalla:[\s\S]*?view\.setLayoutParams\(params\);', '', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
