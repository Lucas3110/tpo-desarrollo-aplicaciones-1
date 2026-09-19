package com.example.ronda.ui.entrega;

import com.example.ronda.data.model.EntregaResponse;
import com.example.ronda.data.model.PublicacionDetalleResponse;
import com.google.gson.Gson;
import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class EnlaceEntregaTest {
    private final Gson gson = new Gson();
    private EntregaResponse entrega(String json) { return gson.fromJson(json, EntregaResponse.class); }

    @Test public void entregaOcultaNoProduceEnlace() {
        PublicacionDetalleResponse response = gson.fromJson(
                "{\"publicacion\":{\"id\":1,\"entrega\":null}}", PublicacionDetalleResponse.class);
        assertNull(EnlaceEntrega.crear(response.getPublicacion().getEntrega()));
    }

    @Test public void respuestaAntiguaSinEntregaNoProduceEnlace() {
        PublicacionDetalleResponse response = gson.fromJson(
                "{\"publicacion\":{\"id\":1}}", PublicacionDetalleResponse.class);
        assertNull(EnlaceEntrega.crear(response.getPublicacion().getEntrega()));
    }

    @Test public void leeEntregaAutorizadaYPriorizaCoordenadas() {
        PublicacionDetalleResponse response = gson.fromJson(
                "{\"publicacion\":{\"entrega\":{\"direccion\":\"Calle 123\","
                        + "\"latitud\":-34.5881,\"longitud\":-58.4106}}}", PublicacionDetalleResponse.class);
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=-34.5881%2C-58.4106&dir_action=navigate",
                EnlaceEntrega.crear(response.getPublicacion().getEntrega()));
    }

    @Test public void direccionSinCoordenadasSeCodificaSinInyectarParametros() {
        EntregaResponse e = entrega("{\"direccion\":\"Av. Córdoba 123 & local #2\"}");
        assertEquals("https://www.google.com/maps/dir/?api=1&destination=Av.+C%C3%B3rdoba+123+%26+local+%232&dir_action=navigate",
                EnlaceEntrega.crear(e));
    }

    @Test public void parIncompletoUsaDireccion() {
        assertEquals("Calle 123", entrega("{\"direccion\":\"Calle 123\",\"latitud\":-34}").getDestino());
        assertNull(EnlaceEntrega.crear(entrega("{\"latitud\":-34}")));
        assertNull(EnlaceEntrega.crear(entrega("{\"longitud\":-58}")));
    }

    @Test public void datosVaciosOInvalidosNoAbrenMapa() {
        assertNull(EnlaceEntrega.crear(entrega("{}")));
        assertNull(EnlaceEntrega.crear(entrega("{\"direccion\":\"   \"}")));
        assertNull(EnlaceEntrega.crear(entrega("{\"latitud\":91,\"longitud\":0}")));
        assertNull(EnlaceEntrega.crear(entrega("{\"latitud\":0,\"longitud\":181}")));
    }

    @Test public void ceroYLimitesSonCoordenadasValidas() {
        assertTrue(entrega("{\"latitud\":0,\"longitud\":0}").tieneDestino());
        assertTrue(entrega("{\"latitud\":-90,\"longitud\":180}").tieneDestino());
    }

    @Test public void coordenadasNoDependenDelIdiomaDelTelefono() {
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("es", "AR"));
            assertEquals("-34.5,-58.5", entrega("{\"latitud\":-34.5,\"longitud\":-58.5}").getDestino());
        } finally { Locale.setDefault(anterior); }
    }
}
