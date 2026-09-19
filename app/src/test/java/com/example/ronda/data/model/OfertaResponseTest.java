package com.example.ronda.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Verifica que los modelos de ofertas mapean el JSON REAL que devuelve la API
 * (respuestas capturadas del backend del equipo corriendo en local). Corre en
 * la JVM con ./gradlew test.
 *
 * Es el mismo Gson que usa Retrofit en la app: si un campo cambia de nombre
 * o de tipo en el backend, esto lo delata antes que un onFailure en el celular.
 */
public class OfertaResponseTest {

    private final Gson gson = new Gson();

    /** Los JSON van con comillas simples para no llenar el archivo de barras. */
    private static String json(String conComillasSimples) {
        return conComillasSimples.replace('\'', '"');
    }

    /** POST /publicaciones/12/ofertas: la oferta del comprador. */
    private static final String OFERTA_DEL_COMPRADOR = json("{'oferta':{"
            + "'id':1,'monto':150000,'mensaje':'150 y lo retiro hoy',"
            + "'estado':'PENDIENTE','estadoTexto':'Pendiente',"
            + "'origen':'COMPRADOR','esContraoferta':false,'contraofertaDeId':null,"
            + "'respondidaEn':null,'expiraEn':'2026-09-21T14:07:09.000Z',"
            + "'creadoEn':'2026-09-19T14:07:09.000Z',"
            + "'autor':{'id':1,'nombre':'Sofía Ramírez'}"
            + "}}");

    /** POST /ofertas/1/contraoferta: el vendedor propone otro precio. */
    private static final String CONTRAOFERTA_DEL_VENDEDOR = json("{'oferta':{"
            + "'id':2,'monto':165000,'mensaje':'165 y te lo dejo',"
            + "'estado':'PENDIENTE','estadoTexto':'Pendiente',"
            + "'origen':'VENDEDOR','esContraoferta':true,'contraofertaDeId':1,"
            + "'respondidaEn':null,'expiraEn':'2026-09-21T14:07:09.000Z',"
            + "'creadoEn':'2026-09-19T14:07:09.000Z',"
            + "'autor':{'id':1,'nombre':'Sofía Ramírez'}"
            + "}}");

    /** GET /publicaciones/12/ofertas visto por la vendedora. */
    private static final String LISTA_DEL_VENDEDOR = json("{'ofertas':["
            + "{'id':2,'monto':165000,'mensaje':null,'estado':'PENDIENTE','estadoTexto':'Pendiente',"
            + " 'origen':'VENDEDOR','esContraoferta':true,'contraofertaDeId':1,"
            + " 'respondidaEn':null,'expiraEn':'2026-09-21T14:07:09.000Z',"
            + " 'creadoEn':'2026-09-19T14:07:09.000Z','autor':{'id':1,'nombre':'Sofía Ramírez'}},"
            + "{'id':1,'monto':150000,'mensaje':'150 y lo retiro hoy','estado':'RECHAZADA','estadoTexto':'Rechazada',"
            + " 'origen':'COMPRADOR','esContraoferta':false,'contraofertaDeId':null,"
            + " 'respondidaEn':'2026-09-19T14:07:09.000Z','expiraEn':'2026-09-21T14:07:09.000Z',"
            + " 'creadoEn':'2026-09-19T14:07:09.000Z','autor':{'id':1,'nombre':'Sofía Ramírez'}}"
            + "],'esVendedor':true}");

    @Test
    public void ofertaDelComprador_mapeaTodosLosCampos() {
        OfertaResponse o = gson.fromJson(OFERTA_DEL_COMPRADOR, OfertaUnicaResponse.class).getOferta();

        assertNotNull(o);
        assertEquals(1, o.getId());
        assertEquals(150000, o.getMonto(), 0.001);
        assertEquals("150 y lo retiro hoy", o.getMensaje());
        assertTrue(o.tieneMensaje());
        assertEquals(OfertaResponse.PENDIENTE, o.getEstado());
        assertEquals("Pendiente", o.getEstadoTexto());
        assertEquals(OfertaResponse.ORIGEN_COMPRADOR, o.getOrigen());
        assertEquals("2026-09-21T14:07:09.000Z", o.getExpiraEn());
        assertNull(o.getRespondidaEn());
        assertEquals("Sofía Ramírez", o.getAutor().getNombre());
        assertTrue(o.isPendiente());
        assertFalse(o.isAceptada());
        assertFalse(o.isEsContraoferta());
        assertNull(o.getContraofertaDeId());
        assertFalse(o.laPropusoElVendedor());
    }

    @Test
    public void contraoferta_apuntaALaOriginalYLaPropusoElVendedor() {
        OfertaResponse o = gson.fromJson(CONTRAOFERTA_DEL_VENDEDOR, OfertaUnicaResponse.class).getOferta();

        assertTrue(o.isEsContraoferta());
        assertEquals(Integer.valueOf(1), o.getContraofertaDeId());
        assertTrue(o.laPropusoElVendedor());
        // El autor del hilo sigue siendo la persona interesada, aunque el
        // monto lo haya propuesto el vendedor: por eso se mira origen.
        assertEquals("Sofía Ramírez", o.getAutor().getNombre());
    }

    @Test
    public void laOfertaDelCompradorLaResponde_elVendedor() {
        OfertaResponse o = gson.fromJson(OFERTA_DEL_COMPRADOR, OfertaUnicaResponse.class).getOferta();

        assertTrue(o.puedoResponderla(true));
        assertFalse(o.puedoResponderla(false));
        assertTrue(o.puedoContraofertarla(true));
        assertFalse(o.puedoContraofertarla(false));
    }

    @Test
    public void laContraofertaDelVendedorLaResponde_elComprador() {
        OfertaResponse o = gson.fromJson(CONTRAOFERTA_DEL_VENDEDOR, OfertaUnicaResponse.class).getOferta();

        assertTrue(o.puedoResponderla(false));
        assertFalse(o.puedoResponderla(true));
        // Sobre una contraoferta ya no se puede volver a contraofertar.
        assertFalse(o.puedoContraofertarla(true));
        assertFalse(o.puedoContraofertarla(false));
    }

    @Test
    public void unaOfertaYaRespondida_noHabilitaNingunBoton() {
        ListaOfertasResponse lista = gson.fromJson(LISTA_DEL_VENDEDOR, ListaOfertasResponse.class);
        OfertaResponse rechazada = lista.getOfertas().get(1);

        assertEquals(OfertaResponse.RECHAZADA, rechazada.getEstado());
        assertFalse(rechazada.isPendiente());
        assertFalse(rechazada.puedoResponderla(true));
        assertFalse(rechazada.puedoContraofertarla(true));
    }

    @Test
    public void listaDeUnaPublicacion_diceSiQuienMiraEsElVendedor() {
        ListaOfertasResponse lista = gson.fromJson(LISTA_DEL_VENDEDOR, ListaOfertasResponse.class);

        assertTrue(lista.isEsVendedor());
        assertEquals(2, lista.getOfertas().size());
        assertFalse(lista.getOfertas().get(0).tieneMensaje());
    }

    @Test
    public void sinPublicacionEnElJson_noRompe() {
        OfertaResponse o = gson.fromJson(json("{'id':9,'monto':5,'estado':'VENCIDA'}"), OfertaResponse.class);

        assertNull(o.getPublicacion());
        assertNull(o.getContraparte());
        assertFalse(o.isEsperaMiRespuesta());
        assertFalse(o.isPendiente());
        assertEquals(OfertaResponse.VENCIDA, o.getEstado());
    }

    @Test
    public void ofertarRequest_conMensaje_mandaLosDosCampos() {
        String cuerpo = gson.toJson(new OfertarRequest(150000, "  150 y lo retiro hoy "));

        assertEquals(json("{'monto':150000.0,'mensaje':'150 y lo retiro hoy'}"), cuerpo);
    }

    @Test
    public void ofertarRequest_sinMensaje_omiteElCampo() {
        assertEquals(json("{'monto':150000.0}"), gson.toJson(new OfertarRequest(150000)));
        assertEquals(json("{'monto':150000.0}"), gson.toJson(new OfertarRequest(150000, "   ")));
        assertEquals(json("{'monto':150000.0}"), gson.toJson(new OfertarRequest(150000, null)));
    }
}
