import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\PublicacionAdapter.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add interface
content = content.replace('public class PublicacionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {', '''public class PublicacionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PublicacionItemResponse pub);
    }
    
    private OnItemClickListener listener;
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
''')

# Add click logic inside bind()
bind_logic = '''
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(pub);
                }
            });
'''
content = content.replace('if (pub.getDistanciaMetros() != null) {', bind_logic + '\n            if (pub.getDistanciaMetros() != null) {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
