package de.bischinger.jallablog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.vertx.core.Vertx.vertx;
import static java.lang.Thread.currentThread;

public class Main {

    public static void main(String[] args) throws IOException {
        vertx().deployVerticle(BlogRestVerticle.class.getName());
    }
}