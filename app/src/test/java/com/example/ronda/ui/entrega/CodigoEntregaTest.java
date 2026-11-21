package com.example.ronda.ui.entrega;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Punto 12: la limpieza y el filtro rapido del codigo leido del QR. */
public class CodigoEntregaTest {

    @Test
    public void normalizaLoQueAgregaUnLectorOUnTipeo() {
        assertEquals("RONDA-QNFG3E54", CodigoEntrega.normalizar("  ronda-qnfg3e54\n"));
        assertEquals("RONDA-QNFG3E54", CodigoEntrega.normalizar("RONDA - QNFG 3E54"));
        assertEquals("", CodigoEntrega.normalizar(null));
        assertEquals("", CodigoEntrega.normalizar("   "));
    }

    @Test
    public void reconoceLosCodigosDeRonda() {
        assertTrue(CodigoEntrega.pareceDeRonda("RONDA-QNFG3E54"));
        assertTrue(CodigoEntrega.pareceDeRonda("ronda-qnfg3e54 "));
        // El de demo que deja el seed del backend.
        assertTrue(CodigoEntrega.pareceDeRonda("RONDA-PRUEBA23"));
    }

    @Test
    public void descartaLoQueNoEsUnQrDeEntrega() {
        assertFalse(CodigoEntrega.pareceDeRonda("https://ejemplo.com/promo"));
        assertFalse(CodigoEntrega.pareceDeRonda("WIFI:T:WPA;S:MiRed;P:1234;;"));
        assertFalse(CodigoEntrega.pareceDeRonda("RONDA-"));
        assertFalse(CodigoEntrega.pareceDeRonda("RONDA-ESTE-CODIGO-ES-DEMASIADO-LARGO"));
        assertFalse(CodigoEntrega.pareceDeRonda(""));
        assertFalse(CodigoEntrega.pareceDeRonda(null));
    }
}
