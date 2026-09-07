# Revisión de las branches del Punto 4

> Para el dev que hizo las branches (y para su asistente de IA).
> Documento autocontenido: se puede pegar entero en un chat.

Branches revisadas:

| Repo | Branch |
|---|---|
| `tpo-desarrollo-aplicaciones-1` (front) | `feature/app-detalle-publicacion` |
| `tpo-ronda-backend` | `feature/backend-fotos-reales` |

**Qué se hizo para revisar:** se compiló el front (`gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`), se corrió `npm run db:setup` y `npm run db:seed` de la branch del backend contra una base real, y se pasó una regresión de 37 chequeos sobre los 6 puntos de la consigna. **35 pasaron.** Los 2 que fallaron son los bugs 3 y 4 de más abajo.

---

## Lo que está bien

Vale decirlo antes de la lista de correcciones:

- **La pantalla de detalle respeta las convenciones del proyecto**: `@AndroidEntryPoint`, `@Inject`, `enqueue()` (nunca `execute()`), chequeo de `estaVivo()` antes de tocar la UI, `onCreateView` sólo infla y `onViewCreated` busca vistas. Eso está bien hecho.
- **El layout del detalle** cubre lo que pide el enunciado: galería, descripción, categoría, estado, precio, fecha, vendedor con reputación.
- **Las fotos reales en el seed** son una mejora genuina: mucho mejor que los placeholders grises para mostrar la app.
- **El `FotosAdapter`** con RecyclerView horizontal es el enfoque correcto para la galería.

Los problemas de abajo son de configuración, datos y prolijidad — no de diseño.

---

# BLOQUEANTES

Cinco cosas que hay que arreglar antes de mergear.

## 1 · La app no conecta desde el emulador

**Archivo:** `app/src/main/java/com/example/ronda/di/NetworkModule.java`

```java
// como quedó (roto)
private static final String URL_EMULADOR = "http://localhost:3000/";

// como tiene que ser
private static final String URL_EMULADOR = "http://10.0.2.2:3000/";
```

**Por qué:** dentro del emulador de Android, `localhost` es **el propio emulador**, no la PC. `10.0.2.2` es el alias que el emulador redirige al `localhost` de la máquina anfitriona. Con `localhost` todas las requests fallan con `ECONNREFUSED` y la app muestra "no pudimos conectarnos".

El comentario que quedó justo arriba de esa línea ya lo advertía:

> `Ojo: "localhost" NO sirve, porque desde el emulador apunta al emulador.`

Si probaste en un celular físico y por eso lo cambiaste: para eso está la otra constante, `URL_RED_LOCAL`, que hay que poner con la IP de la PC en la WiFi (la imprime el backend al arrancar). El método `getBaseUrl()` elige sola cuál usar según dónde corra.

**Cómo verificar:** correr la app en el emulador con el backend levantado y hacer login.

---

## 2 · La columna `rol` nunca se crea

**Archivo:** `sql/01_schema.sql`

Se agregó `rol ENUM('USER','ADMIN')` **adentro** del `CREATE TABLE IF NOT EXISTS usuarios`. Esa tabla ya existe en la base de todo el equipo, así que el `CREATE TABLE IF NOT EXISTS` **no hace nada** y la columna nunca aparece.

**Comprobado:** se corrió `npm run db:setup` sobre una base existente y después:

```sql
SELECT COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='ronda' AND TABLE_NAME='usuarios' AND COLUMN_NAME='rol';
-- resultado: 0
```

Por eso hizo falta el `add_rol_column.js` suelto en la raíz: es el parche manual para lo que la migración debería haber hecho sola.

**Cómo se arregla:** el proyecto ya tiene la convención de migraciones numeradas que `db:setup` corre en orden. Hay que:

1. **Revertir** el cambio en `01_schema.sql` (dejarlo como estaba).
2. Crear `sql/07_roles.sql`:

```sql
-- =============================================================
--  Roles de usuario
-- =============================================================
USE ronda;

-- Se agrega sólo si todavía no existe, para que el script se pueda
-- volver a correr sin fallar.
SET @existe := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'ronda' AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'rol'
);
SET @sql := IF(@existe = 0,
  "ALTER TABLE usuarios ADD COLUMN rol ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER' AFTER email_verificado",
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

3. **Borrar** `add_rol_column.js`: con la migración ya no hace falta.

**Regla general del proyecto:** nunca se edita un `.sql` ya aplicado. Para cambiar el esquema se agrega un archivo nuevo con el número siguiente. `db:setup` los corre en orden alfabético y todos son idempotentes.

**Cómo verificar:** correr `npm run db:setup` dos veces seguidas sobre una base existente. No tiene que dar error, y la columna tiene que aparecer.

---

## 3 · El usuario admin del seed no queda como admin

**Archivo:** `scripts/seed-demo.js`

```js
const [res] = await pool.query(
  `INSERT INTO usuarios (email, password_hash, nombre, telefono, zona_id, email_verificado)
   VALUES (?, ?, ?, ?, ?, 1)`,
  [u.email, passwordHash, u.nombre, u.telefono, zonaId, u.rol || 'USER']
);
```

Contá: la lista tiene **6 columnas**, el `VALUES` tiene **5 `?`** más el literal `1`, y se pasan **6 parámetros**. `rol` no está ni en la lista de columnas ni como `?`, así que el sexto parámetro se descarta en silencio y la columna toma su valor por defecto.

**Comprobado:** después de crear la columna a mano y correr el seed, `admin@ronda.com` queda con `rol = 'USER'`.

**Cómo se arregla:**

```js
const [res] = await pool.query(
  `INSERT INTO usuarios (email, password_hash, nombre, telefono, zona_id, email_verificado, rol)
   VALUES (?, ?, ?, ?, ?, 1, ?)`,
  [u.email, passwordHash, u.nombre, u.telefono, zonaId, u.rol || 'USER']
);
```

(Se agrega `rol` a las columnas y un `?` más al final.)

---

## 4 · Se rompieron los datos de demostración

**Archivo:** `scripts/seed-demo.js`

El usuario admin se agregó **al principio** del array `USUARIOS`. Las publicaciones se asignan por posición (`p.v` es el índice del vendedor), así que meter uno adelante corrió todo un lugar.

**Comprobado** comparando `main` contra la branch, con la base recreada de cero en cada caso:

| Usuario | En `main` | En la branch |
|---|---|---|
| `admin@ronda.com` | no existía | **4 publicaciones + la reputación 4,5 ★** |
| `sofia.demo@ronda.app` | 4 pubs, 4,5 ★ | 4 publicaciones, sin reputación |
| `martin.demo@ronda.app` | 4 pubs | 4 publicaciones |
| `carla.demo@ronda.app` | 4 pubs | **0 publicaciones** |

Una cuenta administrativa ficticia terminó siendo la vendedora con mejor reputación, y Carla se quedó sin publicaciones. Eso afecta a todo el equipo: el listado, el perfil público y los filtros muestran datos raros.

**Cómo se arregla (mínimo):** mover el admin **al final** del array:

```js
const USUARIOS = [
  { email: 'sofia.demo@ronda.app',  nombre: 'Sofía Ramírez',  telefono: '11 4444-1111', zona: 'Palermo' },
  { email: 'martin.demo@ronda.app', nombre: 'Martín Sosa',    telefono: '11 4444-2222', zona: 'Quilmes' },
  { email: 'carla.demo@ronda.app',  nombre: 'Carla Benítez',  telefono: '11 4444-3333', zona: 'Villa Urquiza' },
  // El admin va último: las publicaciones se asignan por índice (p.v),
  // así que agregar usuarios al principio corre a todos los demás.
  { email: 'admin@ronda.com', nombre: 'Admin Ronda', telefono: '11 0000-0000', zona: 'Palermo', rol: 'ADMIN' },
];
```

**Cómo verificar:**

```sql
SELECT u.email, COUNT(p.id) AS publicaciones
  FROM usuarios u LEFT JOIN publicaciones p ON p.vendedor_id = u.id
 WHERE u.email LIKE '%ronda%' GROUP BY u.id;
```

Los tres usuarios demo tienen que tener 4 cada uno, y el admin 0.

---

## 5 · Hay 50 archivos sueltos commiteados

**Backend — 21 archivos en la raíz:**

```
add_rol_column.js   check_photos.js     fix_authService.py   fix_auth_middleware.py
fix_node.js         fix_or.py           fix_photo.py         fix_regex.py
fix_seed.py         fix_seed2.py        fix_seed3.py         fix_seed_array.py
fix_urlFoto.js      fix_urlFoto.py      fix_usuarioDto.py    fix_zona.py
update_photo.js     update_photo2.js    update_photo3.js     update_photo4.js
update_photo_picsum.js
```

**Front — 29 archivos, incluido `logcat.txt` de 1,6 MB:**

```
fix_abrir_detalle.py  fix_action.py        fix_adapter.py       fix_adapter2.py
fix_adapter_click.py  fix_api_service.py   fix_categoria.py     fix_category_click.py
fix_category_click2.py fix_detalle_hilt.py fix_enc.py          fix_error.py
fix_glide.py          fix_home_nav.py      fix_home_xml.py      fix_id.py
fix_id36.py           fix_id48.py          fix_java.py          fix_listeners.py
fix_quotes.py         fix_seed.py          fix_url.py           fix_xml.py
overwrite_xml.py      search_logcat.py     search_logcat2.py    search_logcat_fatal.py
logcat.txt            ← 1,6 MB
```

Son scripts descartables de una sola vez (el patrón `fix_id.py`, `fix_id36.py`, `fix_id48.py` lo deja claro) y un log de depuración. **Todo eso lo va a ver el profesor.**

**Cómo se arregla:**

```bash
git rm --cached <cada archivo>
```

y después borrarlos del disco. Para que no vuelvan a colarse, agregar al `.gitignore` de cada repo:

```gitignore
# Scripts temporales y logs de depuración
fix_*.py
fix_*.js
update_photo*.js
search_logcat*.py
overwrite_*.py
add_rol_column.js
check_photos.js
logcat.txt
*.log
```

> **De fondo:** si una IA te propone crear un script para parchear un archivo, casi siempre conviene pedirle que edite el archivo directamente. El script es andamiaje: sirve en el momento y después estorba.

---

# PROBLEMAS MENORES

Ninguno rompe la app, pero varios se ven o los va a marcar el profesor.

## 6 · Texto corrupto que el usuario ve en pantalla

**Archivo:** `DetallePublicacionFragment.java`

| Línea | Dice | Debería decir |
|---|---|---|
| 96 | `"Abrir gestiA3n de publicaciA3n..."` | `"Abrir gestión de publicación..."` |
| 122 | `"OcurriA3 un error inesperado"` | `"Ocurrió un error inesperado"` |
| 97 | `"Ver perfil pAoblico..."` | `"Ver perfil público..."` |

Y en `LoginFragment.java:93` un comentario quedó como `Â¿sigue sirviendo?`.

Son acentos que se rompieron al guardar el archivo con el encoding equivocado. **Los tres primeros aparecen en pantalla.**

**Cómo se arregla:** corregir los textos y asegurarse de que los archivos se guarden en **UTF-8 sin BOM**. En Android Studio: *File → File Properties → File Encoding → UTF-8*.

## 7 · BOM al inicio de dos XML

`app/src/main/AndroidManifest.xml` e `app/src/main/res/layout/item_foto.xml` arrancan con un BOM (`﻿`) antes del `<?xml`. Compila, pero es incorrecto: la declaración XML tiene que ser el primer byte del archivo. Mismo origen que el punto anterior.

## 8 · El botón "Guardar" miente

**Archivo:** `DetallePublicacionFragment.java:94`

```java
btnGuardar.setOnClickListener(v ->
    Toast.makeText(requireContext(), "Guardado en favoritos!", Toast.LENGTH_SHORT).show());
```

Dice que guardó, pero no llama a ningún endpoint. El backend ya tiene `POST /api/publicaciones/:id/favorito` funcionando.

Si va a quedar como placeholder por ahora, que el texto sea honesto (`"Próximamente"`). Pero conviene implementarlo: son cinco líneas y el endpoint ya existe.

Lo mismo aplica a los otros botones (`Preguntar`, `Ofertar`, `Ver perfil`, `Gestionar`), que también son Toasts.

## 9 · Se borró la documentación de `PublicacionApiService`

El archivo tenía Javadoc explicando cada parámetro del listado (qué significa cada filtro, qué pasa si `precioMax < precioMin`, por qué los tipos son `Integer` y no `int`). Quedó todo borrado y sólo se agregó el método nuevo.

Conviene restaurar los comentarios de `main` y agregarle Javadoc también al método nuevo.

## 10 · Glide entró hardcodeado

**Archivo:** `app/build.gradle.kts`

```kotlin
// como quedó
implementation("com.github.bumptech.glide:glide:4.16.0")
```

El proyecto usa **version catalog** para todas las dependencias, igual que el repo de la cátedra. Corresponde:

En `gradle/libs.versions.toml`:
```toml
[versions]
glide = "4.16.0"

[libraries]
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
```

En `app/build.gradle.kts`:
```kotlin
implementation(libs.glide)
```

## 11 · `usesCleartextTraffic="true"` en el manifest

**Archivo:** `AndroidManifest.xml`

Es redundante: el proyecto ya resuelve el HTTP en claro con `networkSecurityConfig`, que además tiene una versión **sólo para debug** (`app/src/debug/res/xml/`) que permite cualquier host, mientras la de release mantiene una lista restrictiva.

Poner `usesCleartextTraffic="true"` en el manifest principal debilita también la build de release. Conviene sacarlo.

## 12 · Los modelos nuevos no usan `@SerializedName`

**Archivo:** `PublicacionDetalleResponse.java`

Ninguno de los campos tiene la anotación. Hoy funciona porque los nombres coinciden, pero:

- Es la convención del proyecto y **la que enseñó el profesor** en su clase de Retrofit.
- Si algún día se activa la minificación, R8 renombra los campos y el parseo se rompe en silencio.

Además `precio` está declarado como `String` y la API lo devuelve como número. Gson lo tolera, pero el resto de la app usa `double` (ver `PublicacionItemResponse`) y con `String` no se puede formatear el precio con separador de miles.

```java
@SerializedName("precio")
private double precio;
```

## 13 · Textos hardcodeados en el Java

Los Toasts y el `String.format("Reputación: %.1f estrellas...")` están escritos directo en el `.java`. El resto de la app los tiene en `strings.xml`. Conviene moverlos.

## 14 · `requerirRol` se define pero nunca se usa

**Archivo:** `src/middlewares/auth.js`

Se creó el middleware pero no está aplicado en ninguna ruta. Es código muerto: o se usa en algún endpoint, o se saca.

## 15 · El mensaje del commit no describe lo que hace

El commit del backend dice *"Actualizacion de seed con fotos reales de productos"*, pero además modifica el middleware de autenticación, el esquema de la base, `authService` y los DTOs.

Conviene que el mensaje refleje todo lo que entra, o mejor: separar en dos commits (uno para las fotos del seed, otro para los roles).

---

# SOBRE EL ALCANCE: los roles no son parte del Punto 4

Esto no es un bug, es una diferencia de interpretación que conviene aclarar antes de seguir.

El enunciado del Punto 4 dice:

> **Detalle de la Publicación:**
> - Galería con todas las fotos del artículo, descripción completa, categoría, estado, precio y fecha de publicación.
> - Datos del vendedor con su reputación y acceso a su perfil público.
> - **Acciones disponibles según quién esté mirando:** si es un interesado, puede preguntar, ofertar y guardar la publicación; si es el propio vendedor, accede a la gestión de su publicación.

El "según quién esté mirando" es un rol **contextual respecto de esa publicación** — sos el vendedor de *esta* publicación o sos un interesado. **Eso ya estaba resuelto**: el backend calcula y devuelve un objeto `acciones` en `GET /api/publicaciones/:id`:

```json
"esMia": false,
"acciones": {
  "puedePreguntar": true,
  "puedeOfertar": true,
  "puedeGuardar": true,
  "puedeGestionar": false,
  "requiereSesion": false
}
```

Un rol global `USER` / `ADMIN` es otra cosa: es una jerarquía de permisos en toda la plataforma. **No lo pide la consigna**, y tal como quedó no se usa en ningún lado.

> Puede venir de un machete viejo del equipo que decía "Punto 4 backend: agregar roles de usuario". Era una interpretación que no coincide con el enunciado.

**Sugerencia:** sacar los roles de esta branch. Si el equipo los quiere igual para el futuro, que vayan en una branch aparte, completos (con migración, con el seed correcto y con `requerirRol` efectivamente aplicado en alguna ruta). Mezclarlos acá hace que el PR del Punto 4 sea más difícil de revisar y agrega superficie que no se pidió.

---

# LO QUE FALTA PARA COMPLETAR EL PUNTO 4

La pantalla muestra los datos, que es la mitad del enunciado. Falta que las acciones hagan algo. **Todos los endpoints ya existen en el backend:**

| Botón | Endpoint que ya está listo |
|---|---|
| Preguntar | `POST /api/publicaciones/:id/preguntas` con `{ texto }` |
| Ver preguntas | `GET /api/publicaciones/:id/preguntas` (público) |
| Ofertar | `POST /api/publicaciones/:id/ofertas` con `{ monto }` |
| Guardar | `POST /api/publicaciones/:id/favorito` |
| Quitar de favoritos | `DELETE /api/publicaciones/:id/favorito` |
| Ver perfil público | `GET /api/usuarios/:id/perfil` — el id viene en `publicacion.vendedor.id` |
| Gestionar (vendedor) | `PATCH /api/publicaciones/:id/estado` con `{ estado }` |

**Un detalle:** el detalle ya devuelve `esFavorito` cuando mandás token, así que el botón puede arrancar con el estado correcto (corazón lleno o vacío) sin una consulta extra.

**Errores que conviene contemplar** (el backend los devuelve con un `codigo` estable):

- `ES_TU_PUBLICACION` (403) — preguntar u ofertar en la propia
- `PUBLICACION_NO_ACTIVA` (400) — está pausada o vendida
- `OFERTA_MAYOR_AL_PRECIO` (400) — la oferta supera el precio publicado
- `TOKEN_FALTANTE` (401) — hay que mandar a login

---

# CÓMO VERIFICAR QUE QUEDÓ BIEN

```bash
# Backend
npm run db:setup      # dos veces seguidas: no debe fallar
npm run db:seed
npm run dev
```

```sql
-- La columna existe
SELECT COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA='ronda' AND TABLE_NAME='usuarios' AND COLUMN_NAME='rol';   -- 1

-- El admin es admin
SELECT email, rol FROM usuarios WHERE email='admin@ronda.com';                  -- ADMIN

-- Los datos demo están bien repartidos
SELECT u.email, COUNT(p.id) FROM usuarios u
  LEFT JOIN publicaciones p ON p.vendedor_id=u.id
 WHERE u.email LIKE '%ronda%' GROUP BY u.id;    -- sofia 4, martin 4, carla 4, admin 0
```

```bash
# Front
gradlew :app:assembleDebug     # BUILD SUCCESSFUL
git status                     # sin archivos fix_*.py ni logcat.txt
```

Y en el emulador: login → Home → tocar una publicación → tiene que abrir el detalle con la galería y los datos del vendedor.

---

# RESUMEN

| # | Qué | Dónde | Gravedad |
|---|---|---|---|
| 1 | `localhost` en vez de `10.0.2.2` | front · `NetworkModule` | **bloqueante** |
| 2 | La columna `rol` nunca se crea | back · `01_schema.sql` | **bloqueante** |
| 3 | El admin del seed queda como `USER` | back · `seed-demo.js` | **bloqueante** |
| 4 | Índices corridos: Carla sin publicaciones | back · `seed-demo.js` | **bloqueante** |
| 5 | 50 archivos sueltos + `logcat.txt` de 1,6 MB | ambos | **bloqueante** |
| 6 | Texto corrupto visible en pantalla | front · `DetallePublicacionFragment` | medio |
| 7 | BOM en dos XML | front | bajo |
| 8 | El botón "Guardar" no guarda | front | medio |
| 9 | Se borró el Javadoc de `PublicacionApiService` | front | bajo |
| 10 | Glide sin version catalog | front · `build.gradle.kts` | bajo |
| 11 | `usesCleartextTraffic` redundante | front · manifest | bajo |
| 12 | Modelos sin `@SerializedName`, `precio` como `String` | front | medio |
| 13 | Textos hardcodeados en el Java | front | bajo |
| 14 | `requerirRol` sin usar | back · `auth.js` | bajo |
| 15 | Mensaje de commit incompleto | back | bajo |

Lo bueno de la lista: **todo es mecánico**. La arquitectura de la pantalla está bien; lo que hay que arreglar es configuración, datos y limpieza.
