import os
file_path = r'app\src\main\java\com\example\ronda\data\model\PublicacionDetalleResponse.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

import re
# Insert Categoria field
content = content.replace('private String publicadoEn;', 'private String publicadoEn;\n        private Categoria categoria;')
content = content.replace('public String getPublicadoEn() { return publicadoEn; }', 'public String getPublicadoEn() { return publicadoEn; }\n        public Categoria getCategoria() { return categoria; }')

# Insert Categoria class
categoria_class = '''
    public static class Categoria {
        private String nombre;
        public String getNombre() { return nombre; }
    }
'''
content = content.replace('public static class Foto {', categoria_class + '\n    public static class Foto {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

# Update DetallePublicacionFragment.java
file_path2 = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

content2 = content2.replace('tvEstadoArticulo.setText(pub.getEstadoArticuloTexto() + " | " + fechaSimple);', 'String cat = pub.getCategoria() != null ? pub.getCategoria().getNombre() : "";\n        tvEstadoArticulo.setText(cat + " | " + pub.getEstadoArticuloTexto() + " | " + fechaSimple);')

with open(file_path2, 'w', encoding='utf-8') as f:
    f.write(content2)
