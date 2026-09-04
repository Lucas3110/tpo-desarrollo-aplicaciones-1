import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\HomeFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add navigation to fragment
import re
content = content.replace('adapter = new PublicacionAdapter();', '''adapter = new PublicacionAdapter();
        adapter.setOnItemClickListener(pub -> {
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putInt("publicacionId", pub.getId());
            Navigation.findNavController(getView()).navigate(R.id.action_home_to_detallePublicacion, bundle);
        });''')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
