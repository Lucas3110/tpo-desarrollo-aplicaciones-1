import os
import re

file_path = r'app\src\main\java\com\example\ronda\ui\home\HomeFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = '''    private void abrirDetalle(PublicacionItemResponse publicacion) {
        Toast.makeText(requireContext(),
                getString(R.string.home_detalle_pendiente, publicacion.getTitulo()),
                Toast.LENGTH_SHORT).show();
    }'''

replacement = '''    private void abrirDetalle(PublicacionItemResponse publicacion) {
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putInt("publicacionId", publicacion.getId());
        androidx.navigation.Navigation.findNavController(getView()).navigate(R.id.action_home_to_detallePublicacion, bundle);
    }'''

content = content.replace(target, replacement)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
