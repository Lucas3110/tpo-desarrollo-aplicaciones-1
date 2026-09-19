package com.example.ronda.data.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class PublicacionRequestTest {
    private JsonObject serializar(String direccion, Double lat, Double lng) {
        return new Gson().toJsonTree(new PublicacionRequest("Mesa", "Madera", 2,
                100, "USADO", 9, Collections.singletonList("content://document/foto"),
                direccion, lat, lng)).getAsJsonObject();
    }

    @Test public void enviaDireccionCoordenadasYFotosLocales() {
        JsonObject json = serializar(" Av. Santa Fe 3253 ", -34.5881, -58.4106);
        assertEquals("Av. Santa Fe 3253", json.get("direccion").getAsString());
        assertEquals(-34.5881, json.get("latitud").getAsDouble(), 0.000001);
        assertEquals(-58.4106, json.get("longitud").getAsDouble(), 0.000001);
        assertEquals("content://document/foto", json.getAsJsonArray("fotos").get(0).getAsString());
    }

    @Test public void permiteDireccionSinGeocodificacion() {
        JsonObject json = serializar("Calle 123", null, null);
        assertTrue(json.has("direccion"));
        assertFalse(json.has("latitud"));
        assertFalse(json.has("longitud"));
    }

    @Test public void nuncaEnviaUnaCoordenadaSola() {
        for (Double[] par : new Double[][]{{-34.0, null}, {null, -58.0}}) {
            JsonObject json = serializar("Calle 123", par[0], par[1]);
            assertFalse(json.has("latitud"));
            assertFalse(json.has("longitud"));
        }
    }

    @Test public void descartaCoordenadasInvalidas() {
        for (Double[] par : new Double[][]{{91.0, 0.0}, {0.0, -181.0},
                {Double.NaN, 0.0}, {0.0, Double.POSITIVE_INFINITY}}) {
            JsonObject json = serializar("Calle 123", par[0], par[1]);
            assertFalse(json.has("latitud"));
            assertFalse(json.has("longitud"));
        }
        assertTrue(PublicacionRequest.coordenadasValidas(-90.0, 180.0));
    }

    @Test public void permiteOmitirDireccion() {
        assertFalse(serializar("  ", null, null).has("direccion"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaDireccionMayorA255() {
        serializar(new String(new char[256]).replace('\0', 'a'), null, null);
    }
}
