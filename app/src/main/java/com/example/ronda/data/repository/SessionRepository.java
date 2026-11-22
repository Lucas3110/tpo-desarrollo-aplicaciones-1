package com.example.ronda.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.ronda.data.model.ZonaResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Guarda la sesión en el celular (Punto 1). Hace de TokenManager: ninguna
 * pantalla sabe dónde se guarda el token, se lo piden a esta clase.
 *
 * Usa SharedPreferences: un archivo de pares clave-valor que viene con
 * Android (no es una librería extra) y queda en el disco del celular, así que
 * el token sobrevive a cerrar y volver a abrir la app. Eso es lo que permite
 * el auto-login. La copia que usa la huella va encriptada, en un archivo
 * aparte.
 *
 * Es @Singleton: hay una sola en toda la app. Se pide con @Inject, y el
 * contexto lo pone Hilt con @ApplicationContext, el de la app entera: usar el
 * de un Fragment guardaría una referencia a una pantalla que después se
 * destruye.
 */
@Singleton
public class SessionRepository {

    private static final String TAG = "SessionRepository";

    // Nombre del archivo y claves con las que se guarda cada dato.
    private static final String ARCHIVO = "sesion_ronda";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_EMAIL = "email";
    private static final String CLAVE_ZONA_ID = "zonaId";
    private static final String CLAVE_ZONA_NOMBRE = "zonaNombre";
    private static final String CLAVE_BIOMETRIA = "biometriaActivada";
    private static final int SIN_ZONA = -1;

    /** Archivo aparte, encriptado, solo para la copia del token de la huella. */
    private static final String ARCHIVO_CIFRADO = "sesion_ronda_cifrada";
    private static final String CLAVE_TOKEN_CIFRADO = "tokenCifrado";

    private final Context contexto;
    private final SharedPreferences prefs;
    /** El archivo encriptado. Se crea recién la primera vez que se usa. */
    private SharedPreferences prefsCifradas;

    /** Hilt llama a este constructor (por el @Inject) y le pasa el contexto. */
    @Inject
    public SessionRepository(@ApplicationContext Context contexto) {
        this.contexto = contexto;
        this.prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /**
     * Guarda el token y el email al iniciar sesión. apply() escribe en el
     * disco en segundo plano, sin trabar la pantalla.
     */
    public void guardarSesion(String token, String email) {
        prefs.edit()
                .putString(CLAVE_TOKEN, token)
                .putString(CLAVE_EMAIL, email)
                .apply();
    }

    /** El token. Lo lee el interceptor en cada pedido. null si no hay sesión. */
    public String getToken() {
        return prefs.getString(CLAVE_TOKEN, null);
    }

    /** El email de la última sesión. Sirve para precargar el login. */
    public String getEmail() {
        return prefs.getString(CLAVE_EMAIL, null);
    }

    /**
     * El id de la persona, sacado del propio token: un JWT tiene tres partes
     * separadas por puntos, y la del medio trae el id en el campo "sub".
     */
    public Integer getUsuarioId() {
        String token = getToken();
        if (token == null) return null;
        try {
            String[] partes = token.split("\\.");
            if (partes.length < 2) return null;
            String payload = new String(android.util.Base64.decode(partes[1], android.util.Base64.URL_SAFE), "UTF-8");
            org.json.JSONObject json = new org.json.JSONObject(payload);
            return json.getInt("sub");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Zona de la persona, para "Solo mi zona" y "Más cercanas" del Home.
     * Se guarda al iniciar sesión, se refresca en cada auto-login y la
     * actualiza Mi perfil cuando cambia. Con null (usuario sin zona) se borra
     * lo guardado.
     */
    public void guardarZona(ZonaResponse zona) {
        SharedPreferences.Editor editor = prefs.edit();
        if (zona == null) {
            editor.remove(CLAVE_ZONA_ID).remove(CLAVE_ZONA_NOMBRE);
        } else {
            editor.putInt(CLAVE_ZONA_ID, zona.getId()).putString(CLAVE_ZONA_NOMBRE, zona.getNombre());
        }
        editor.apply();
    }

    public boolean tieneZona() {
        return prefs.getInt(CLAVE_ZONA_ID, SIN_ZONA) != SIN_ZONA;
    }

    /** Id de la zona, o null si la persona no configuró ninguna. */
    public Integer getZonaId() {
        int id = prefs.getInt(CLAVE_ZONA_ID, SIN_ZONA);
        return id == SIN_ZONA ? null : id;
    }

    public String getZonaNombre() {
        return prefs.getString(CLAVE_ZONA_NOMBRE, null);
    }

    /** Hay sesión si hay un token guardado. */
    public boolean haySesion() {
        return getToken() != null;
    }

    /** Valor exacto que espera el header Authorization. */
    public String getBearer() {
        return "Bearer " + getToken();
    }

    /**
     * Para las rutas con token opcional (el listado de publicaciones): si no
     * hay sesión devuelve null y Retrofit directamente no manda el header,
     * en vez de mandar "Bearer null".
     */
    public String getBearerOpcional() {
        return haySesion() ? getBearer() : null;
    }

    /**
     * Cierra la sesión: se va el token y los datos de la persona.
     *
     * NO se borra la preferencia de biometría. Es una preferencia del
     * dispositivo, no de la sesión: si alguien ya eligió "entrar con huella",
     * volver a preguntárselo en cada login es molesto y da la sensación de
     * que la app no se acuerda de nada.
     *
     * La copia cifrada del token sí se borra, y esa es la parte que importa:
     * después de cerrar sesión, la huella no puede resucitar la sesión
     * anterior. Para volver a entrar hay que poner la contraseña o pedir un
     * código.
     */
    public void cerrarSesion() {
        // El email queda: no es un secreto y sirve para precargar el formulario.
        prefs.edit()
                .remove(CLAVE_TOKEN)
                .remove(CLAVE_ZONA_ID)
                .remove(CLAVE_ZONA_NOMBRE)
                .apply();
        borrarTokenCifrado();
    }

    /**
     * Borra TODO, incluida la preferencia de biometría y el email.
     *
     * Se usa al dar de alta una cuenta nueva. La diferencia con cerrarSesion()
     * está en quién vuelve: si cerrás sesión, lo normal es que vuelvas vos, y
     * por eso se conserva la preferencia. Si en cambio se está creando otra
     * cuenta, la persona es otra, y dejarle la huella y el token cifrado del
     * anterior significa que la app te ofrece entrar con la sesión de alguien
     * más.
     */
    public void olvidarTodo() {
        prefs.edit().clear().apply();
        borrarTokenCifrado();
    }

    // -----------------------------------------------------------------
    // Punto 1 · desbloqueo con biometría
    // -----------------------------------------------------------------
    //
    // La huella sola no alcanza: el sensor devuelve true o false, pero no sabe
    // nada de la sesión en el backend. Lo que hace la biometría acá es abrir el
    // candado sobre un token que ya estaba guardado, igual que la app del
    // banco cuando pide huella para ver el saldo.
    //
    // Por eso el token vive en dos lugares:
    //   plano   (sesion_ronda)         -> se lee en cada pedido para el header
    //                                     Authorization; tiene que ser rápido.
    //   cifrado (sesion_ronda_cifrada) -> solo si la persona activó biometría.
    //                                     Es el que se recupera DESPUÉS de
    //                                     validar la huella.
    //
    // La diferencia importa: el archivo plano lo puede leer cualquiera con
    // acceso al almacenamiento interno (un celular rooteado, un adb backup).
    // La copia cifrada usa una llave del Keystore del dispositivo, que nunca
    // sale del hardware, así que aunque alguien lea el archivo no puede
    // reactivar la sesión sin pasar antes por el sensor.

    /** ¿La persona eligió entrar con la huella? */
    public boolean esBiometriaActivada() {
        return prefs.getBoolean(CLAVE_BIOMETRIA, false);
    }

    /** Guarda la preferencia. Si se desactiva, también se borra la copia cifrada. */
    public void setBiometriaActivada(boolean activada) {
        prefs.edit().putBoolean(CLAVE_BIOMETRIA, activada).apply();
        if (!activada) borrarTokenCifrado();
    }

    /** Guarda la copia encriptada del token (la que se recupera con la huella). */
    public void guardarTokenCifrado(String token) {
        SharedPreferences cifradas = prefsCifradas();
        if (cifradas == null) return;
        cifradas.edit().putString(CLAVE_TOKEN_CIFRADO, token).apply();
    }

    /** La lee LoginFragment después de validar la huella. */
    public String getTokenCifrado() {
        SharedPreferences cifradas = prefsCifradas();
        return cifradas == null ? null : cifradas.getString(CLAVE_TOKEN_CIFRADO, null);
    }

    private void borrarTokenCifrado() {
        SharedPreferences cifradas = prefsCifradas();
        if (cifradas != null) cifradas.edit().remove(CLAVE_TOKEN_CIFRADO).apply();
    }

    /**
     * Construye (una sola vez) el archivo encriptado.
     *
     * Devuelve null si el dispositivo no puede crearlo: hay equipos viejos o
     * con el Keystore roto donde EncryptedSharedPreferences falla. En ese caso
     * la app sigue andando sin biometría en vez de cerrarse: es una comodidad,
     * no el mecanismo de autenticación.
     */
    private SharedPreferences prefsCifradas() {
        if (prefsCifradas != null) return prefsCifradas;
        try {
            // La llave maestra queda en el Keystore del celular y nunca sale
            // del hardware. Con ella se encriptan las claves y los valores.
            MasterKey llave = new MasterKey.Builder(contexto)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefsCifradas = EncryptedSharedPreferences.create(
                    contexto,
                    ARCHIVO_CIFRADO,
                    llave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return prefsCifradas;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "No pude abrir las preferencias cifradas", e);
            return null;
        }
    }
}
