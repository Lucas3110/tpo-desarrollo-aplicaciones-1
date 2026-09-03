import os

file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if 'Toast.makeText(requireContext(), "Falta el ID de la publicaci' in lines[i]:
        lines[i] = '            Toast.makeText(requireContext(), "Falta el ID de la publicación", Toast.LENGTH_SHORT).show();\n'
    elif 'tvReputaciA3n: %.1f estrellas' in lines[i]:
        lines[i] = '                tvReputacion.setText(String.format("Reputación: %.1f estrellas (%d operaciones)",\n'
    elif 'tvReputacion.setText("AA' in lines[i]:
        lines[i] = '                tvReputacion.setText("Aún no tiene calificaciones");\n'

with open(file_path, 'w', encoding='utf-8', newline='') as f:
    f.writelines(lines)
