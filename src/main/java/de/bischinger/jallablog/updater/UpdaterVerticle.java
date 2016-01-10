package de.bischinger.jallablog.updater;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

/**
 * Created by bischofa on 10/01/16.
 */
public class UpdaterVerticle extends AbstractVerticle {
    @Override
    public void start() {
        EventBus eb = vertx.eventBus();

        eb.consumer("updater.fire", message -> {

            System.out.println("Received message: " + message.body());
            try {
                Thread.sleep(5000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Receiver ready!");
    }
}
