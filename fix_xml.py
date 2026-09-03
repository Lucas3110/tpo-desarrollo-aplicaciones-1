import os
import re

file_path = r'app\src\main\res\layout\fragment_detalle_publicacion.xml'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = {
    'DescripciA3n': 'Descripción',
    'InformaciA3n': 'Información',
    'publicaciA3n': 'publicación',
    'ReputaciA3n': 'Reputación',
    'CondiciA3n': 'Condición',
    'pA\ufffdblico': 'público',
    'pA-xeles': 'píxeles',
    'BenA-tez': 'Benítez',
    'GalerA-a': 'Galería',
    'TA-tulo': 'Título',
    'DinA\ufffdmicas': 'Dinámicas'
}

for old, new in replacements.items():
    content = content.replace(old, new)

# Also fix the weird ones shown by Select-String
content = content.replace('pAblico', 'público')
content = content.replace('DinAmicas', 'Dinámicas')
content = content.replace('An', 'Aún')

with open(file_path, 'w', encoding='utf-8', newline='') as f:
    f.write(content)
