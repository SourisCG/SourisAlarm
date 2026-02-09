package com.souris;

import javafx.scene.media.AudioClip;
import javax.sound.sampled.*;
import java.io.File;
import java.net.URL;

public class AudioService {

    private AudioClip audioClip;
    private volatile boolean sonando = false;
    private File archivoPersonalizado;
    private String sonidoDefaultURI;
    private double volumen = 1.0; // 100% por defecto

    public AudioService() {
        URL recurso = getClass().getResource("alarma.mp3");
        if (recurso != null) this.sonidoDefaultURI = recurso.toExternalForm();
    }

    public void setArchivoPersonalizado(File archivo) {
        this.archivoPersonalizado = archivo;
    }

    public void setVolumen(double v) {
        this.volumen = v;
        if (audioClip != null) audioClip.setVolume(volumen);
    }

    public void reproducir() {
        if (sonando) return;
        sonando = true;

        Thread hilo = new Thread(() -> {
            try {
                if (archivoPersonalizado != null && archivoPersonalizado.exists()) {
                    playClip(archivoPersonalizado.toURI().toString());
                } else if (sonidoDefaultURI != null) {
                    playClip(sonidoDefaultURI);
                } else {
                    playBeepLoop();
                }
            } catch (Exception e) {
                playBeepLoop();
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        sonando = false;
        if (audioClip != null && audioClip.isPlaying()) audioClip.stop();
    }

    private void playClip(String uri) {
        if (audioClip != null) audioClip.stop();
        audioClip = new AudioClip(uri);
        audioClip.setVolume(volumen);
        audioClip.setCycleCount(AudioClip.INDEFINITE);
        audioClip.play();
    }

    private void playBeepLoop() {
        try {
            while (sonando) {
                for (int i = 0; i < 3 && sonando; i++) {
                    generarTono(1000, 150);
                    Thread.sleep(100);
                }
                if (sonando) Thread.sleep(800);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generarTono(int hz, int msecs) throws Exception {
        float sampleRate = 8000f;
        byte[] buf = new byte[1];
        AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        for (int i = 0; i < msecs * 8; i++) {
            double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
            buf[0] = (byte) (Math.sin(angle) * 127.0 * 1.0);
            sdl.write(buf, 0, 1);
        }
        sdl.drain();
        sdl.stop();
        sdl.close();
    }
}