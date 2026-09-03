import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\FotosAdapter.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

import re
content = re.sub(r'\.centerCrop\(\)\n\s*\.into\(holder\.ivFoto\);', '.centerCrop().placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_dialog_alert).into(holder.ivFoto);', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
