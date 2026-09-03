import sys
import os

files = [
    r'app\src\main\res\layout\fragment_detalle_publicacion.xml',
    r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # XML replacements
    content = content.replace('DescripciAA3n', 'Descripción')
    content = content.replace('InformaciAA3n', 'Información')
    content = content.replace('pAAblico', 'público')
    content = content.replace('publicaciAA3n', 'publicación')
    content = content.replace('pAA-xeles', 'píxeles')
    content = content.replace('BenAA-tez', 'Benítez')
    content = content.replace('ReputaciAA3n', 'Reputación')
    content = content.replace('GalerAA-a', 'Galería')
    content = content.replace('TAA-tulo', 'Título')
    content = content.replace('CondiciAA3n', 'Condición')
    content = content.replace('DinAAmicas', 'Dinámicas')
    
    # Java replacements
    content = content.replace('Falta el ID de la publicaciA\'A+?TA?sA,A3n', 'Falta el ID de la publicación')
    content = content.replace('ReputaciA\'A+?TA?sA,A3n: %.1f \nA\'A,AA?1A.?oAAA?sAA,A (%d operaciones)', 'Reputación: %.1f estrellas (%d operaciones)')
    content = content.replace('AA\'A+?TA?sA,An no tiene calificaciones', 'Aún no tiene calificaciones')

    with open(file_path, 'w', encoding='utf-8', newline='') as f:
        f.write(content)
