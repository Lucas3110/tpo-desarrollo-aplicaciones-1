import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\FotosAdapter.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('params.width = parent.getWidth();', 'params.width = parent.getWidth() > 0 ? parent.getWidth() : ViewGroup.LayoutParams.MATCH_PARENT;')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
