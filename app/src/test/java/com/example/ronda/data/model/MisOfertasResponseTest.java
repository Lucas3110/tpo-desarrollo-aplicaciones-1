package com.example.ronda.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * GET /ofertas/mias con la respuesta real capturada del backend para la
 * compradora: la oferta que envio (ya rechazada porque le contraofertaron) y
 * la contraoferta que recibio, pendiente de su respuesta.
 */
public class MisOfertasResponseTest {

    private final Gson gson = new Gson();

    private static String json(String conComillasSimples) {
        return conComillasSimples.replace('\'', '"');
    }

    private static final String MIAS = json("{"
            + "'enviadas':[{'id':1,'monto':150000,'mensaje':'150 y lo retiro hoy',"
            + "  'estado':'RECHAZADA','estadoTexto':'Rechazada','origen':'COMPRADOR',"
            + "  'esContraoferta':false,'contraofertaDeId':null,"
            + "  'respondidaEn':'2026-09-19T14:07:09.000Z','expiraEn':'2026-09-21T14:07:09.000Z',"
            + "  'creadoEn':'2026-09-19T14:07:09.000Z','autor':{'id':1,'nombre':'Sofía Ramírez'},"
            + "  'publicacion':{'id':12,'titulo':'Monitor Samsung 24\\' curvo','precio':175000,"
            + "    'estado':'ACTIVA','fotoPrincipal':'https://images.unsplash.com/photo-1527443154391'},"
            + "  'contraparte':{'id':3,'nombre':'Carla Benítez'},'esperaMiRespuesta':false}],"
            + "'recibidas':[{'id':2,'monto':165000,'mensaje':'165 y te lo dejo',"
            + "  'estado':'PENDIENTE','estadoTexto':'Pendiente','origen':'VENDEDOR',"
            + "  'esContraoferta':true,'contraofertaDeId':1,"
            + "  'respondidaEn':null,'expiraEn':'2026-09-21T14:07:09.000Z',"
            + "  'creadoEn':'2026-09-19T14:07:09.000Z','autor':{'id':1,'nombre':'Sofía Ramírez'},"
            + "  'publicacion':{'id':12,'titulo':'Monitor Samsung 24\\' curvo','precio':175000,"
            + "    'estado':'ACTIVA','fotoPrincipal':'https://images.unsplash.com/photo-1527443154391'},"
            + "  'contraparte':{'id':3,'nombre':'Carla Benítez'},'esperaMiRespuesta':true}]"
            + "}");

    @Test
    public void separaEnviadasDeRecibidas() {
        MisOfertasResponse mias = gson.fromJson(MIAS, MisOfertasResponse.class);

        assertEquals(1, mias.getEnviadas().size());
        assertEquals(1, mias.getRecibidas().size());
    }

    @Test
    public void cuentaLasRecibidasQueEsperanMiRespuesta() {
        MisOfertasResponse mias = gson.fromJson(MIAS, MisOfertasResponse.class);

        assertEquals(1, mias.getPendientesRecibidas());
        assertTrue(mias.getRecibidas().get(0).isEsperaMiRespuesta());
        assertFalse(mias.getEnviadas().get(0).isEsperaMiRespuesta());
    }

    @Test
    public void cadaOfertaTraeSuPublicacionYLaOtraParte() {
        OfertaResponse enviada = gson.fromJson(MIAS, MisOfertasResponse.class).getEnviadas().get(0);

        OfertaResponse.Publicacion pub = enviada.getPublicacion();
        assertNotNull(pub);
        assertEquals(12, pub.getId());
        assertEquals("Monitor Samsung 24\" curvo", pub.getTitulo());
        assertEquals(175000, pub.getPrecio(), 0.001);
        assertTrue(pub.estaActiva());
        assertEquals("Carla Benítez", enviada.getContraparte().getNombre());
    }

    @Test
    public void laRecibidaEsLaContraofertaDelVendedor() {
        OfertaResponse recibida = gson.fromJson(MIAS, MisOfertasResponse.class).getRecibidas().get(0);

        assertTrue(recibida.isEsContraoferta());
        assertTrue(recibida.laPropusoElVendedor());
        assertEquals(Integer.valueOf(1), recibida.getContraofertaDeId());
        assertTrue(recibida.isPendiente());
    }

    @Test
    public void sinListasEnElJson_devuelveListasVacias() {
        MisOfertasResponse mias = gson.fromJson(json("{}"), MisOfertasResponse.class);

        assertNotNull(mias.getEnviadas());
        assertTrue(mias.getEnviadas().isEmpty());
        assertTrue(mias.getRecibidas().isEmpty());
        assertEquals(0, mias.getPendientesRecibidas());
    }
}
