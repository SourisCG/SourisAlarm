package com.souris;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Alarma> alarmasGuardadas;
    public boolean modoOscuroActivado;
    public double nivelVolumen;
    public String rutaImagenFondo;
    public String nombreFuente;
    public String rutaSonido;

    public AppState() {
        this.alarmasGuardadas = new ArrayList<>();
        this.modoOscuroActivado = false; // Default: Claro
        this.nivelVolumen = 1.0;
        this.rutaImagenFondo = null;
        this.nombreFuente = "Segoe UI";
        this.rutaSonido = null;
    }
}