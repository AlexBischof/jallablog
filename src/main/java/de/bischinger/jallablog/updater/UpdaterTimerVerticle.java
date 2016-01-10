package de.bischinger.jallablog.updater;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by bischofa on 10/01/16.
 */
public class UpdaterTimerVerticle extends AbstractVerticle {
    @Override
    public void start() {
        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(2), time -> {
            LoggerFactory.getLogger(getClass()).info("Update timer fired.");
            vertx.eventBus().send("updater.fire", "periodic");
        });
    }
}
