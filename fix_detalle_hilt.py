import os
file_path = r'app\src\main\java\com\example\ronda\ui\home\DetallePublicacionFragment.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove RetrofitClient import
content = content.replace('import com.example.ronda.data.network.RetrofitClient;\n', '')

# Add Hilt imports
hilt_imports = '''import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import com.example.ronda.data.network.PublicacionApiService;
import com.example.ronda.data.repository.SessionRepository;
'''
content = content.replace('import retrofit2.Call;', hilt_imports + '\nimport retrofit2.Call;')

# Add Hilt annotations and injections
content = content.replace('public class DetallePublicacionFragment extends Fragment {', '''@AndroidEntryPoint
public class DetallePublicacionFragment extends Fragment {

    @Inject
    PublicacionApiService publicacionApi;

    @Inject
    SessionRepository sesion;
''')

# Update api call
content = content.replace('RetrofitClient.getPublicacionApi().getDetallePublicacion(new com.example.ronda.data.repository.SessionRepository(requireContext()).getBearer(), publicacionId)', 'publicacionApi.getDetallePublicacion(sesion.getBearer(), publicacionId)')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
