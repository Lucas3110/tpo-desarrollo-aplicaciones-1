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
 * Guarda la sesión en el celular.
 *
 * SharedPreferences viene incluido en Android (no es una librería extra) y
 * persiste en disco, así que el token sobrevive a cerrar y volver a abrir la
 * app. Eso es lo que permite el auto-login.
 *
 * Con Hilt ya no se instancia a mano: se pide con @Inject y el
 * @ApplicationContext lo provee el framework, así que nadie tiene que
 * acordarse de pasar el contexto correcto (usar el del Fragment filtraría una
 * referencia a una pantalla ya destruida).
 */
@Singleton
public class SessionRepository {

    private static final String TAG = "SessionRepository";

    private static final String ARCHIVO = "sesion_ronda";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_EMAIL = "email";
    private static final String CLAVE_ZONA_ID = "zonaId";
    private static final String CLAVE_ZONA_NOMBRE = "zonaNombre";
    private static final String CLAVE_BIOMETRIA = "biometriaActivada";
    private static final int SIN_ZONA = -1;

    /** Archivo aparte, cifrado, sólo para la copia del token del Punto 1. */
    private static final String ARCHIVO_CIFRADO = "sesion_ronda_cifrada";
    private static final String CLAVE_TOKEN_CIFRADO = "tokenCifrado";

    private final Context contexto;
    private final SharedPreferences prefs;
    private SharedPreferences prefsCifradas;

    @Inject
    public SessionRepository(@ApplicationContext Context contexto) {
        this.contexto = contexto;
        this.prefs = contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String token, String email) {
        prefs.edit()
                .putString(CLAVE_TOKEN, token)
                .putString(CLAVE_EMAIL, email)
                .apply();
    }

    public String getToken() {
        return prefs.getString(CLAVE_TOKEN, null);
    }

    public String getEmail() {
        return prefs.getString(CLAVE_EMAIL, null);
    }

    /**
     * Zona de la persona, para "Solo mi zona" y "Mas cercanas" del Home.
     * Se guarda al iniciar sesion y se refresca en cada auto-login. Si el
     * Punto 2 (editar perfil) cambia la zona, tiene que llamar a esto tambien.
     * Con null (usuario sin zona) se borra lo guardado.
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

    /** Id de la zona, o null si la persona no configuro ninguna. */
    public Integer getZonaId() {
        int id = prefs.getInt(CLAVE_ZONA_ID, SIN_ZONA);
        return id == SIN_ZONA ? null : id;
    }

    public String getZonaNombre() {
        return prefs.getString(CLAVE_ZONA_NOMBRE, null);
    }

    public boolean haySesion() {
        return getToken() != null;
    }

    /** Valor exacto que espera la cabecera Authorization. */
    public String getBearer() {
        return "Bearer " + getToken();
    }

    /**
     * Para las rutas con token opcional (el listado de publicaciones): si no
     * hay sesion devuelve null y Retrofit directamente no manda la cabecera,
     * en vez de mandar "Bearer null".
     */
    public String getBearerOpcional() {
        return haySesion() ? getBearer() : null;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
        borrarTokenCifrado();
    }

    // -----------------------------------------------------------------
    // Punto 1 · desbloqueo con biometría
    // -----------------------------------------------------------------
    //
    // La huella sola no alcanza: el sensor devuelve true o false, pero no sabe
    // nada de la sesión en el backend. Lo que hace la biometría acá es abrir el
    // candado sobre un token que ya estaba guardado — igual que la app del
    // banco cuando pide huella para ver el saldo.
    //
    // Por eso el token vive en dos lugares:
    //   plano   (sesion_ronda)         -> se lee en cada request para el header
    //                                     Authorization; tiene que ser rápido.
    //   cifrado (sesion_ronda_cifrada) -> sólo si la persona activó biometría.
    //                                     Es el que se recupera DESPUÉS de
    //                                     validar la huella.
    //
    // La diferencia importa: el archivo plano lo puede leer cualquiera con
    // acceso al almacenamiento interno (un celular rooteado, un adb backup).
    // La copia cifrada usa una llave del Keystore del dispositivo, que nunca
    // sale del hardware, así que aunque alguien lea el archivo no puede
    // reactivar la sesión sin pasar antes por el sensor.

    public boolean esBiometriaActivada() {
        return prefs.getBoolean(CLAVE_BIOMETRIA, false);
    }

    public void setBiometriaActivada(boolean activada) {
        prefs.edit().putBoolean(CLAVE_BIOMETRIA, activada).apply();
        if (!activada) borrarTokenCifrado();
    }

    public void guardarTokenCifrado(String token) {
        SharedPreferences cifradas = prefsCifradas();
        if (cifradas == null) return;
        cifradas.edit().putString(CLAVE_TOKEN_CIFRADO, token).apply();
    }

    public String getTokenCifrado() {
        SharedPreferences cifradas = prefsCifradas();
        return cifradas == null ? null : cifradas.getString(CLAVE_TOKEN_CIFRADO, null);
    }

    private void borrarTokenCifrado() {
        SharedPreferences cifradas = prefsCifradas();
        if (cifradas != null) cifradas.edit().remove(CLAVE_TOKEN_CIFRADO).apply();
    }

    /**
     * Construye (una sola vez) el archivo cifrado.
     *
     * Devuelve null si el dispositivo no puede crearlo — hay equipos viejos o
     * con el Keystore roto donde EncryptedSharedPreferences falla. En ese caso
     * la app sigue andando sin biometría en vez de crashear: es una comodidad,
     * no el mecanismo de autenticación.
     */
    private SharedPreferences prefsCifradas() {
        if (prefsCifradas != null) return prefsCifradas;
        try {
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
