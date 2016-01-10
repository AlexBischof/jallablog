package de.bischinger.jallablog;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.ws.rs.client.Client;
import java.io.IOException;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Created by bischofa on 07/01/16.
 */
public class BlogImporter {
    public static void main(String[] args) throws IOException {

        JsonObject blog = new JsonObject();
        blog.put("body", "Hallo Welt");
        JsonArray comments = new JsonArray();
        blog.put("comments", comments);
        JsonObject comment = new JsonObject();
        comments.add(comment);
        comment.put("name", "Alex Bischof");

        for (int i = 0; i < 10_000; i++) {
            String titleId = System.currentTimeMillis() + "" + i;
            blog.put("title", titleId);

            Client client = newClient();
            client.target("http://127.0.0.1:8080/blogs/").path(titleId).request(APPLICATION_JSON).
                    put(entity(blog.toString(), APPLICATION_JSON));
            client.close();
        }
    }
}