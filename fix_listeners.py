import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

listeners = '''
        btnPreguntar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir chat de preguntas...", Toast.LENGTH_SHORT).show());
        btnOfertar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir flujo de oferta...", Toast.LENGTH_SHORT).show());
        btnGuardar.setOnClickListener(v -> Toast.makeText(requireContext(), "Guardado en favoritos!", Toast.LENGTH_SHORT).show());
        btnGestionar.setOnClickListener(v -> Toast.makeText(requireContext(), "Abrir gestiA3n de publicaciA3n...", Toast.LENGTH_SHORT).show());
        btnVerPerfil.setOnClickListener(v -> Toast.makeText(requireContext(), "Ver perfil pAoblico...", Toast.LENGTH_SHORT).show());

'''

content = content.replace('if (publicacionId != -1) {', listeners + '        if (publicacionId != -1) {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
