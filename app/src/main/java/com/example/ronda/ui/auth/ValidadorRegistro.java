package com.example.ronda.ui.auth;

import androidx.annotation.StringRes;

import com.example.ronda.R;

/**
 * Validaciones locales del formulario de alta.
 *
 * Estan aca, y no sueltas en el Fragment, por dos motivos: se pueden leer de
 * un vistazo junto a las reglas del backend, y se reusan si manana otra
 * pantalla pide los mismos datos (por ejemplo el "editar perfil" del Punto 2).
 *
 * OJO: esto NO reemplaza la validacion del servidor. Sirve para dar una
 * respuesta inmediata y ahorrar un viaje a la red, pero cualquiera puede
 * saltearse la app y pegarle a la API con Postman. Los mismos limites estan
 * repetidos en src/services/authService.js del backend.
 */
public final class ValidadorRegistro {

    public static final int NOMBRE_MAX = 30;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 40;
    /** Mismo limite que TELEFONO_MAX en src/services/usuarioService.js. */
    public static final int TELEFONO_MAX = 30;

    private ValidadorRegistro() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Resultado de validar un campo: o esta bien, o trae el id del texto de
     * error que hay que mostrar.
     */
    public static class Resultado {
        private final Integer mensajeError;

        private Resultado(Integer mensajeError) {
            this.mensajeError = mensajeError;
        }

        public static Resultado valido() {
            return new Resultado(null);
        }

        public static Resultado invalido(@StringRes int mensaje) {
            return new Resultado(mensaje);
        }

        public boolean esValido() {
            return mensajeError == null;
        }

        @StringRes
        public int getMensajeError() {
            return mensajeError != null ? mensajeError : 0;
        }
    }

    /**
     * El nombre es obligatorio: el perfil publico del Punto 2 y las
     * publicaciones necesitan mostrar de quien son.
     */
    public static Resultado validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();

        if (limpio.isEmpty()) {
            return Resultado.invalido(R.string.error_nombre_vacio);
        }
        if (limpio.length() > NOMBRE_MAX) {
            return Resultado.invalido(R.string.error_nombre_largo);
        }
        for (int i = 0; i < limpio.length(); i++) {
            if (Character.isDigit(limpio.charAt(i))) {
                return Resultado.invalido(R.string.error_nombre_con_numeros);
            }
        }
        // Letras de cualquier idioma (con acentos y enie), espacios,
        // apostrofos y guiones. Tiene que empezar con una letra.
        if (!limpio.matches("^\\p{L}[\\p{L} '’-]*$")) {
            return Resultado.invalido(R.string.error_nombre_invalido);
        }
        return Resultado.valido();
    }

    public static Resultado validarEmail(String email) {
        String limpio = email == null ? "" : email.trim();

        if (limpio.isEmpty()) {
            return Resultado.invalido(R.string.error_email_vacio);
        }
        // El mismo criterio que usa el backend: algo, arroba, algo, punto, algo.
        if (!limpio.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Resultado.invalido(R.string.error_email_invalido);
        }
        return Resultado.valido();
    }

    public static Resultado validarPassword(String password) {
        String valor = password == null ? "" : password;

        if (valor.length() < PASSWORD_MIN) {
            return Resultado.invalido(R.string.error_password_corta);
        }
        if (valor.length() > PASSWORD_MAX) {
            return Resultado.invalido(R.string.error_password_larga);
        }
        return Resultado.valido();
    }

    /**
     * Telefono de contacto del Punto 2. Es opcional: vacio es valido (el
     * backend lo guarda como NULL). Si viene, mismo criterio que
     * src/services/usuarioService.js: numeros, espacios, guiones, parentesis
     * y un "+" inicial, con al menos 6 caracteres.
     */
    public static Resultado validarTelefono(String telefono) {
        String limpio = telefono == null ? "" : telefono.trim();

        if (limpio.isEmpty()) {
            return Resultado.valido();
        }
        if (limpio.length() > TELEFONO_MAX) {
            return Resultado.invalido(R.string.error_telefono_largo);
        }
        if (!limpio.matches("^\\+?[\\d\\s()-]{6,}$")) {
            return Resultado.invalido(R.string.error_telefono_invalido);
        }
        return Resultado.valido();
    }
}
