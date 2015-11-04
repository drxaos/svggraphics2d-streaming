package com.github.drxaos.svgstreaming;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import org.apache.commons.io.IOUtils;
import sun.awt.image.IntegerInterleavedRaster;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class Server extends AbstractVerticle {
    ImageSource source;

    public Server(ImageSource source) {
        this.source = source;
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        router.routeWithRegex("/(.+\\.(html|js|css))?").handler(ctx -> {
            try {
                String path = ctx.request().path().replace("/", "");
                if (path.isEmpty()) {
                    path = "index.html";
                }
                ctx.response().putHeader("Content-Type", "text/html; charset=utf-8").end(
                        IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(path))
                );
            } catch (Exception e) {
                e.printStackTrace();
                ctx.response().end("Error!");
            }
        });

        vertx.createHttpServer().requestHandler(router::accept).websocketHandler(ws -> {
            if (!ws.path().equals("/stream")) {
                ws.reject();
                return;
            }
            final String id = ws.textHandlerID();
            System.out.println("registering new connection with id: " + id + "");

            System.out.println("Starting streaming...");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            final boolean[] ready = new boolean[]{true};
            final int[] count = new int[]{0};

            ws.closeHandler(event -> System.out.println("Done" + count[0]));

            final AtomicReference<String> prev = new AtomicReference<String>("");

            ws.handler(data -> {
                try {
                    if (!ready[0]) {
                        ws.close();
                        return;
                    }
                    ready[0] = false;

                    String s = new String(data.getBytes());
                    if (s.equals("i")) {
                        // new image request
                        final long start = System.currentTimeMillis();

                        String image = source.getImage();
                        if (image.equals(prev.get())) {
                            image = "e";
                        } else {
                            prev.set(image);
                        }

                        System.out.println("Frame " + count[0]++ + " / " + image.length() + " bytes / " + (System.currentTimeMillis() - start) + " ms");
                        ws.writeFinalTextFrame(image);
                        ready[0] = true;
                        stream.reset();
                    } else if (s.equals("s")) {
                        ws.writeFinalTextFrame("d" + source.getWidth() + "x" + source.getHeight());
                        ready[0] = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ws.close();
                }
            });
        }).listen(8888);
    }
}