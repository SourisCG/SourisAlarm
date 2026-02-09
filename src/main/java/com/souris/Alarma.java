package com.souris;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * MODELO DE DATOS
 * Representa una alarma individual con su hora y días de repetición.
 * Implementa Serializable para poder guardarse en un archivo.
 */
public class Alarma implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final LocalTime hora;
    private final boolean[] diasActivos; // Array de 7 booleanos (Domingo a Sábado)
    private boolean activa;

    public Alarma(LocalTime hora, boolean[] diasActivos) {
        this.hora = hora;
        this.diasActivos = Arrays.copyOf(diasActivos, 7);
        this.activa = true;
    }

    public LocalTime getHora() { return hora; }
    
    /**
     * Verifica si la alarma debe sonar en el día de la semana indicado.
     * @param diaSemanaJava 1 (Lunes) a 7 (Domingo)
     */
    public boolean debeSonarHoy(int diaSemanaJava) {
        // Ajuste: Java usa 1=Lunes...7=Domingo.
        // Nuestro array empieza en 0=Domingo, 1=Lunes...
        int indice = (diaSemanaJava == 7) ? 0 : diaSemanaJava;
        return diasActivos[indice];
    }
    
    public boolean isDiaActivo(int indice) {
        if (indice >= 0 && indice < 7) return diasActivos[indice];
        return false;
    }
    
    /**
     * Si no hay ningún día marcado, se asume que es una alarma de "una sola vez".
     */
    public boolean esUnaSolaVez() {
        for (boolean dia : diasActivos) {
            if (dia) return false;
        }
        return true;
    }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() { return hora.toString(); }
}