package com.example.ronda.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Verifica que los modelos del Punto 3 mapean el JSON REAL que devuelve la
 * API (los ejemplos son respuestas capturadas del backend, ver CONTEXTO.md
 * del repo tpo-ronda-backend). Corre en la JVM con ./gradlew test.
 *
 * Es el mismo Gson que usa Retrofit en la app, asi que si un campo cambia
 * de nombre o de tipo en el backend, este test lo delata antes que un
 * onFailure en el celular.
 */
public class PaginaPublicacionesResponseTest {

    private final Gson gson = new Gson();

    /** GET /publicaciones con token: viene esFavorito. */
    private static final String PAGINA_CON_TOKEN = "{"
            + "\"items\":[{"
            + "  \"id\":92,"
            + "  \"titulo\":\"Monitor Samsung 24\\\" curvo\","
            + "  \"precio\":175000,"
            + "  \"estadoArticulo\":\"USADO\","
            + "  \"estadoArticuloTexto\":\"Usado\","
            + "  \"estado\":\"ACTIVA\","
            + "  \"categoria\":{\"id\":2,\"nombre\":\"Computación\"},"
            + "  \"zona\":{\"id\":9,\"nombre\":\"Villa Urquiza\"},"
            + "  \"fotoPrincipal\":\"https://picsum.photos/seed/ronda-92-0/800/600\","
            + "  \"cantidadFotos\":2,"
            + "  \"creadoEn\":\"2026-08-28T21:08:24.000Z\","
            + "  \"esFavorito\":false"
            + "}],"
            + "\"pagina\":1,\"limite\":20,\"total\":12,\"totalPaginas\":1,\"hayMas\":false"
            + "}";

    /** GET /publicaciones sin token: no viene esFavorito; ademas una publicacion sin fotos. */
    private static final String PAGINA_SIN_TOKEN = "{"
            + "\"items\":[{"
            + "  \"id\":93,\"titulo\":\"Sin foto\",\"precio\":1234.5,"
            + "  \"estadoArticulo\":\"NUEVO\",\"estadoArticuloTexto\":\"Nuevo\",\"estado\":\"ACTIVA\","
            + "  \"categoria\":{\"id\":1,\"nombre\":\"Celulares\"},"
            + "  \"zona\":{\"id\":6,\"nombre\":\"Palermo\"},"
            + "  \"fotoPrincipal\":null,\"cantidadFotos\":0,"
            + "  \"creadoEn\":\"2026-08-28T21:08:24.000Z\""
            + "}],"
            + "\"pagina\":2,\"limite\":10,\"total\":22,\"totalPaginas\":3,\"hayMas\":true"
            + "}";

    @Test
    public void listado_conToken_mapeaTodosLosCamposDeLaTarjeta() {
        PaginaPublicacionesResponse pagina = gson.fromJson(PAGINA_CON_TOKEN, PaginaPublicacionesResponse.class);

        assertEquals(1, pagina.getPagina());
        assertEquals(20, pagina.getLimite());
        assertEquals(12, pagina.getTotal());
        assertEquals(1, pagina.getTotalPaginas());
        assertFalse(pagina.isHayMas());
        assertEquals(1, pagina.getItems().size());

        PublicacionItemResponse item = pagina.getItems().get(0);
        assertEquals(92, item.getId());
        assertEquals("Monitor Samsung 24\" curvo", item.getTitulo());
        assertEquals(175000, item.getPrecio(), 0.0);
        assertEquals("USADO", item.getEstadoArticulo());
        assertEquals("Usado", item.getEstadoArticuloTexto());
        assertEquals("ACTIVA", item.getEstado());
        assertEquals(2, item.getCategoria().getId());
        assertEquals("Computación", item.getCategoria().getNombre());
        assertEquals(9, item.getZona().getId());
        assertEquals("Villa Urquiza", item.getZona().getNombre());
        assertEquals("https://picsum.photos/seed/ronda-92-0/800/600", item.getFotoPrincipal());
        assertEquals(2, item.getCantidadFotos());
        assertEquals("2026-08-28T21:08:24.000Z", item.getCreadoEn());
        assertFalse(item.isFavorito());
    }

    @Test
    public void listado_sinToken_esFavoritoAusenteEsFalseYFotoNull() {
        PaginaPublicacionesResponse pagina = gson.fromJson(PAGINA_SIN_TOKEN, PaginaPublicacionesResponse.class);

        assertTrue(pagina.isHayMas());
        assertEquals(2, pagina.getPagina());

        PublicacionItemResponse item = pagina.getItems().get(0);
        assertNull(item.getFotoPrincipal());
        assertEquals(0, item.getCantidadFotos());
        assertEquals(1234.5, item.getPrecio(), 0.0);
        // El campo no vino en el JSON: no es "true" ni revienta.
        assertFalse(item.isFavorito());
    }

    @Test
    public void listado_esFavoritoTrue() {
        String json = PAGINA_CON_TOKEN.replace("\"esFavorito\":false", "\"esFavorito\":true");
        PaginaPublicacionesResponse pagina = gson.fromJson(json, PaginaPublicacionesResponse.class);

        assertTrue(pagina.getItems().get(0).isFavorito());
    }

    @Test
    public void categorias_vienenEnvueltasEnUnObjeto() {
        // listarCategorias() del backend devuelve { categorias: [...] }, no una lista pelada.
        String json = "{\"categorias\":[{\"id\":1,\"nombre\":\"Celulares\"},{\"id\":2,\"nombre\":\"Computación\"}]}";
        CategoriasResponse respuesta = gson.fromJson(json, CategoriasResponse.class);

        assertNotNull(respuesta.getCategorias());
        assertEquals(2, respuesta.getCategorias().size());
        assertEquals("Celulares", respuesta.getCategorias().get(0).getNombre());
    }

    @Test
    public void zonas_vienenEnvueltasYConCoordenadasQueLaAppIgnora() {
        // toZonaListadoDto agrega latitud y longitud; ZonaResponse no las declara y Gson las salta.
        String json = "{\"zonas\":[{\"id\":6,\"nombre\":\"Palermo\",\"latitud\":-34.5881,\"longitud\":-58.4306}]}";
        ZonasResponse respuesta = gson.fromJson(json, ZonasResponse.class);

        assertEquals(1, respuesta.getZonas().size());
        assertEquals(6, respuesta.getZonas().get(0).getId());
        assertEquals("Palermo", respuesta.getZonas().get(0).getNombre());
    }
}
