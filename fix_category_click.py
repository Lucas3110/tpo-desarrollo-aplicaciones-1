import os
import re

file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = '''        btnPreguntar.setOnClickListener(v -> mostrarToastEnDesarrollo());
        btnOfertar.setOnClickListener(v -> mostrarToastEnDesarrollo());
        btnFavorito.setOnClickListener(v -> mostrarToastEnDesarrollo());'''

replacement = '''        btnPreguntar.setOnClickListener(v -> mostrarToastEnDesarrollo());
        btnOfertar.setOnClickListener(v -> mostrarToastEnDesarrollo());
        btnFavorito.setOnClickListener(v -> mostrarToastEnDesarrollo());
        tvEstadoArticulo.setOnClickListener(v -> mostrarToastEnDesarrollo());'''

content = content.replace(target, replacement)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
