package com.example.ronda.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Regresion del bug de la PR #8: "zona" llega como objeto { id, nombre } y
 * el modelo la declaraba como String, con lo que Gson tiraba
 * JsonSyntaxException y el login de cualquier usuario con zona caia en
 * onFailure como si no hubiera conexion.
 */
public class SesionResponseTest {

    private final Gson gson = new Gson();

    /** Respuesta real de POST /auth/login para sofia.demo@ronda.app (con zona). */
    private static final String SESION_CON_ZONA = "{"
            + "\"token\":\"eyJhbGciOiJIUzI1NiIs.demo\","
            + "\"usuario\":{"
            + "  \"id\":51,\"email\":\"sofia.demo@ronda.app\",\"nombre\":\"Sofía Ramírez\","
            + "  \"telefono\":\"11 4444-1111\","
            + "  \"zona\":{\"id\":6,\"nombre\":\"Palermo\"},"
            + "  \"emailVerificado\":true,\"creadoEn\":\"2026-08-29T21:08:24.000Z\""
            + "}}";

    /** Usuario recien registrado: todavia no eligio zona. */
    private static final String SESION_SIN_ZONA = SESION_CON_ZONA
            .replace("{\"id\":6,\"nombre\":\"Palermo\"}", "null");

    @Test
    public void sesion_conZona_parseaLaZonaComoObjeto() {
        SesionResponse sesion = gson.fromJson(SESION_CON_ZONA, SesionResponse.class);

        assertEquals("eyJhbGciOiJIUzI1NiIs.demo", sesion.getToken());
        assertEquals("sofia.demo@ronda.app", sesion.getUsuario().getEmail());
        assertEquals(6, sesion.getUsuario().getZona().getId());
        assertEquals("Palermo", sesion.getUsuario().getZona().getNombre());
        assertTrue(sesion.getUsuario().isEmailVerificado());
    }

    @Test
    public void sesion_sinZona_dejaLaZonaEnNull() {
        SesionResponse sesion = gson.fromJson(SESION_SIN_ZONA, SesionResponse.class);

        assertNull(sesion.getUsuario().getZona());
        assertEquals(51, sesion.getUsuario().getId());
    }

    @Test
    public void perfil_deAuthMe_tieneLaMismaFormaDeUsuario() {
        String json = "{\"usuario\":{\"id\":51,\"email\":\"sofia.demo@ronda.app\",\"nombre\":\"Sofía Ramírez\","
                + "\"telefono\":null,\"zona\":{\"id\":6,\"nombre\":\"Palermo\"},\"emailVerificado\":true,"
                + "\"creadoEn\":\"2026-08-29T21:08:24.000Z\"}}";
        PerfilResponse perfil = gson.fromJson(json, PerfilResponse.class);

        assertEquals("Palermo", perfil.getUsuario().getZona().getNombre());
        assertNull(perfil.getUsuario().getTelefono());
    }
}
