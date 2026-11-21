package com.example.ronda.ui.entrega;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lo que la app sabe de un codigo de entrega (Punto 12), sin tocar vistas ni
 * red, para poder probarlo en la JVM con ./gradlew test.
 *
 * El QR fisico contiene el codigo tal cual: "RONDA-" y ocho caracteres. Antes
 * de mandarlo al backend se limpia lo que un lector o un tipeo pueden
 * agregar (espacios, saltos de linea, minusculas) y se descarta rapido lo que
 * claramente no es un QR de Ronda, para no gastar un viaje al servidor en
 * mostrar "codigo invalido" por un QR de wifi o una URL cualquiera.
 */
public final class CodigoEntrega {

    /** RONDA- y de 4 a 14 letras, numeros o guiones: el formato real y el de demo. */
    private static final Pattern FORMATO = Pattern.compile("^RONDA-[A-Z0-9-]{4,14}$");

    private CodigoEntrega() {
    }

    /** Mayusculas y sin espacios. Cadena vacia si no vino nada. */
    public static String normalizar(@Nullable String leido) {
        if (leido == null) return "";
        return leido.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    /**
     * Tiene la forma de un codigo de entrega de Ronda. No dice que exista ni
     * que sea mio: eso lo sabe el backend, que es quien lo emitio.
     */
    public static boolean pareceDeRonda(@Nullable String leido) {
        return FORMATO.matcher(normalizar(leido)).matches();
    }
}
