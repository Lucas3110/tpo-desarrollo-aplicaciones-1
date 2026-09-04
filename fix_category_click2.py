import os

file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = 'btnGuardar.setOnClickListener(v -> Toast.makeText(requireContext(), "Guardado en favoritos!", Toast.LENGTH_SHORT).show());'
replacement = target + '\n        tvEstadoArticulo.setOnClickListener(v -> Toast.makeText(requireContext(), "Navegar a categoria...", Toast.LENGTH_SHORT).show());'

content = content.replace(target, replacement)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
