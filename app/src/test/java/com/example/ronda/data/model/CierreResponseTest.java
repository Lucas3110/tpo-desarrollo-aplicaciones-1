package com.example.ronda.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Punto 12: los modelos del cierre con QR mapean el JSON real del backend
 * (respuestas capturadas de la regresion de la API). Corre en la JVM con
 * ./gradlew test, con el mismo Gson que usa Retrofit.
 */
public class CierreResponseTest {

    private final Gson gson = new Gson();

    private static String json(String conComillasSimples) {
        return conComillasSimples.replace('\'', '"');
    }

    private static final String CIERRE_COMPRADOR = "'cierre':{'operacionId':52,'estado':'PENDIENTE_ENTREGA',"
            + "'estadoTexto':'Entrega pendiente','codigoEntrega':null,'puedoConfirmar':true,"
            + "'aceptadaEn':'2026-11-21T20:28:17.000Z','concretadaEn':null}";

    private static final String CIERRE_VENDEDORA = "'cierre':{'operacionId':52,'estado':'PENDIENTE_ENTREGA',"
            + "'estadoTexto':'Entrega pendiente','codigoEntrega':'RONDA-QNFG3E54','puedoConfirmar':false,"
            + "'aceptadaEn':'2026-11-21T20:28:17.000Z','concretadaEn':null}";

    /** GET /ofertas/mias visto por el comprador: le toca escanear, no ve el codigo. */
    @Test
    public void elCompradorSabeQueLeTocaEscanearPeroNoVeElCodigo() {
        OfertaResponse o = gson.fromJson(json("{'id':31,'monto':158000,'estado':'ACEPTADA','estadoTexto':'Aceptada',"
                + "'origen':'VENDEDOR','esContraoferta':true,'contraofertaDeId':30,"
                + "'publicacion':{'id':12,'titulo':'Monitor Samsung','precio':160000,'estado':'RESERVADA'},"
                + "'contraparte':{'id':1,'nombre':'Sofía Ramírez'},'esperaMiRespuesta':false," + CIERRE_COMPRADOR + "}"),
                OfertaResponse.class);

        assertTrue(o.isAceptada());
        assertNotNull(o.getCierre());
        assertEquals(52, o.getCierre().getOperacionId());
        assertTrue(o.getCierre().estaPendiente());
        assertFalse(o.getCierre().estaConcretada());
        assertTrue(o.getCierre().isPuedoConfirmar());
        assertFalse(o.getCierre().tengoCodigoParaImprimir());
        assertNull(o.getCierre().getConcretadaEn());
        assertTrue(o.getPublicacion().estaReservada());
        assertFalse(o.getPublicacion().estaActiva());
    }

    /** La misma oferta vista por la vendedora: tiene el codigo para imprimir el QR. */
    @Test
    public void laVendedoraVeElCodigoYNoLeTocaConfirmar() {
        OfertaResponse o = gson.fromJson(json("{'id':31,'monto':158000,'estado':'ACEPTADA','estadoTexto':'Aceptada',"
                + "'origen':'VENDEDOR','esperaMiRespuesta':false," + CIERRE_VENDEDORA + "}"), OfertaResponse.class);

        assertTrue(o.getCierre().tengoCodigoParaImprimir());
        assertEquals("RONDA-QNFG3E54", o.getCierre().getCodigoEntrega());
        assertFalse(o.getCierre().isPuedoConfirmar());
        assertEquals("Entrega pendiente", o.getCierre().getEstadoTexto());
    }

    /** Las ofertas que no estan aceptadas no traen cierre; la app no tiene que romperse. */
    @Test
    public void unaOfertaPendienteNoTieneCierre() {
        OfertaResponse o = gson.fromJson(json("{'id':30,'monto':150000,'estado':'PENDIENTE','estadoTexto':'Pendiente',"
                + "'origen':'COMPRADOR','esperaMiRespuesta':true,'cierre':null}"), OfertaResponse.class);
        assertNull(o.getCierre());

        OfertaResponse vieja = gson.fromJson(json("{'id':29,'monto':1000,'estado':'RECHAZADA'}"), OfertaResponse.class);
        assertNull(vieja.getCierre());
    }

    /** GET /publicaciones/:id con una venta en curso, visto por la vendedora. */
    @Test
    public void elDetalleTraeElEstadoDeLaPublicacionYElCierre() {
        PublicacionDetalleResponse r = gson.fromJson(json("{'publicacion':{'id':12,'titulo':'Monitor Samsung',"
                + "'precio':160000,'estado':'RESERVADA','esMia':true,'entregaVisible':true,"
                + "'acciones':{'puedePreguntar':false,'puedeOfertar':false,'puedeGuardar':false,"
                + "'puedeGestionar':true,'requiereSesion':false}," + CIERRE_VENDEDORA + "}}"),
                PublicacionDetalleResponse.class);

        PublicacionDetalleResponse.Publicacion pub = r.getPublicacion();
        assertTrue(pub.estaReservada());
        assertFalse(pub.estaVendida());
        assertNotNull(pub.getCierre());
        assertEquals("RONDA-QNFG3E54", pub.getCierre().getCodigoEntrega());
    }

    /** Un tercero (o una respuesta vieja del backend) no trae cierre ni estado. */
    @Test
    public void elDetalleSinCierreSigueFuncionando() {
        PublicacionDetalleResponse r = gson.fromJson(json("{'publicacion':{'id':12,'titulo':'Monitor','cierre':null}}"),
                PublicacionDetalleResponse.class);
        assertNull(r.getPublicacion().getCierre());
        assertFalse(r.getPublicacion().estaReservada());
    }

    /** POST /operaciones/confirmar-entrega con el QR correcto. */
    @Test
    public void laConfirmacionTraeLaOperacionConcretadaYLaPublicacionVendida() {
        ConfirmarEntregaResponse r = gson.fromJson(json("{'mensaje':'Operación confirmada: recibiste «Monitor Samsung». "
                + "La publicación pasó a vendida y la operación quedó registrada para vos y para Sofía Ramírez.',"
                + "'cierre':{'operacionId':52,'estado':'CONCRETADA','estadoTexto':'Operación confirmada',"
                + "'codigoEntrega':null,'puedoConfirmar':false,'aceptadaEn':'2026-11-21T20:28:17.000Z',"
                + "'concretadaEn':'2026-11-21T20:28:18.000Z'},"
                + "'operacion':{'id':52,'tipo':'COMPRA','fecha':'2026-11-21T20:28:18.000Z','montoFinal':158000,"
                + "'articulo':{'id':12,'titulo':'Monitor Samsung','fotoPrincipal':null},"
                + "'contraparte':{'id':1,'nombre':'Sofía Ramírez'},"
                + "'calificacion':{'puedeCalificar':true,'yaCalifique':false,'diasRestantes':7,"
                + "'misEstrellas':null,'estrellasRecibidas':null}},"
                + "'publicacion':{'id':12,'titulo':'Monitor Samsung','estado':'VENDIDA'}}"),
                ConfirmarEntregaResponse.class);

        assertTrue(r.getMensaje().startsWith("Operación confirmada"));
        assertTrue(r.getCierre().estaConcretada());
        assertFalse(r.getCierre().isPuedoConfirmar());
        assertNotNull(r.getCierre().getConcretadaEn());
        assertEquals("COMPRA", r.getOperacion().getTipo());
        assertEquals(158000, r.getOperacion().getMontoFinal(), 0.001);
        assertEquals("Monitor Samsung", r.getOperacion().getArticulo().getTitulo());
        assertEquals("Sofía Ramírez", r.getOperacion().getContraparte().getNombre());
        assertTrue(r.getOperacion().getCalificacion().isPuedeCalificar());
        assertTrue(r.getPublicacion().estaVendida());
    }

    /** GET /publicaciones/mias: el resumen suma las reservadas. */
    @Test
    public void elResumenDeMisPublicacionesCuentaLasReservadas() {
        MisPublicacionesResponse r = gson.fromJson(json("{'items':[],'pagina':1,'limite':20,'total':5,"
                + "'resumen':{'activas':3,'pausadas':1,'reservadas':1,'vendidas':0,'total':5}}"),
                MisPublicacionesResponse.class);
        assertEquals(1, r.getResumen().getReservadas());
        assertEquals(5, r.getResumen().getTotal());

        // Un backend viejo sin el campo: queda en 0, no revienta.
        MisPublicacionesResponse viejo = gson.fromJson(json("{'items':[],'resumen':{'activas':1,'pausadas':0,'vendidas':0,'total':1}}"),
                MisPublicacionesResponse.class);
        assertEquals(0, viejo.getResumen().getReservadas());
    }
}
