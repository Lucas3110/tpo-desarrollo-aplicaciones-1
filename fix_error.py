import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('getString(R.string.error_generico)', '"OcurriA3 un error inesperado"')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
