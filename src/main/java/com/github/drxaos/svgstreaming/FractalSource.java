package com.github.drxaos.svgstreaming;

import io.vertx.core.Vertx;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public class FractalSource implements ImageSource, Runnable {

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new Server(new FractalSource()));
    }

    float x = 0;
    Thread t;
    boolean running;
    AtomicReference<String> image = new AtomicReference<>("");

    public FractalSource() {
        t = new Thread(this);
        t.setDaemon(true);
        t.start();
        running = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        running = false;
    }

    @Override
    public synchronized String getImage() {
        return image.get();
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 600;
    }

    private void drawTree(Graphics g, int x1, int y1, double angle, int depth, double delta) {
        if (depth == 0) return;
        int x2 = x1 + (int) (Math.cos(Math.toRadians(angle)) * depth * 10.0);
        int y2 = y1 + (int) (Math.sin(Math.toRadians(angle)) * depth * 10.0);
        g.drawLine(x1, y1, x2, y2);
        drawTree(g, x2, y2, angle - delta, depth - 1, delta);
        drawTree(g, x2, y2, angle + delta, depth - 1, delta);
    }

    @Override
    public void run() {
        while (running) {
            x += 0.05;
            synchronized (this) {
                StringBuilder sb = new StringBuilder();
                Graphics g = new SVGGraphics2D(800, 600, sb);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, 800, 600);
                g.setColor(Color.WHITE);
                drawTree(g, 400, 500, -90, 9, Math.sin(x) * 40);
                image.set(sb.toString());
            }
            try {
                Thread.sleep(70);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }
}
