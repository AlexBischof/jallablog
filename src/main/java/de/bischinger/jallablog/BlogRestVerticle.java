/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package de.bischinger.jallablog;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jboss.resteasy.annotations.Status;

import java.io.IOException;
import java.net.UnknownHostException;

import static io.vertx.core.logging.LoggerFactory.getLogger;
import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.net.InetAddress.getByName;
import static org.elasticsearch.client.Requests.getRequest;
import static org.elasticsearch.client.Requests.indexRequest;
import static org.elasticsearch.common.settings.Settings.settingsBuilder;

public class BlogRestVerticle extends AbstractVerticle {

    private String indexName;//= "blogindex";
    private String typeName;// = "blogs";

    private Client client;
    private PropertyHolder propertyHolder;

    @Override
    public void start() throws IOException {

    /*    DeploymentOptions workerOptions = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(new UpdaterTimerVerticle(), workerOptions);
        vertx.deployVerticle(new UpdaterVerticle(), workerOptions);
*/

        propertyHolder = new PropertyHolder();
        indexName = propertyHolder.getProperty("elasticsearch.blog.indexName");
        typeName = propertyHolder.getProperty("elasticsearch.blog.typeName");

        setupElasticsearchClient();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/updater/run").handler(this::handleRunUpdater);
        router.post("/blogs/:name").handler(this::handleAddBlog);
        router.get("/blogs").handler(this::handleListBlogs);

        router.route().handler(StaticHandler.create());

        String httpPort = propertyHolder.getProperty("http.port");
        vertx.createHttpServer().requestHandler(router::accept)
                .listen(parseInt(httpPort));
        getLogger(this.getClass()).info("Listening http on " + httpPort);
    }

    private void handleRunUpdater(RoutingContext routingContext) {
        String blogKey = routingContext.request().getParam("name");
        HttpServerResponse response = routingContext.response();
        if (blogKey == null) {
            sendError(400, response);
        } else {
            JsonObject blog = routingContext.getBodyAsJson();
            if (blog == null) {
                sendError(400, response);
            } else {
                //Map to name to id
                String id = blog.getString("name");
                IndexRequest indexRequest = new IndexRequest(indexName, typeName, id).source(blog.toString());

                vertx.executeBlocking(future -> {
                    client.index(indexRequest).actionGet();
                    future.complete();
                }, res -> response.end());
            }
        }
    }

    @Override
    public void stop() throws Exception {
        client.close();
    }

    private void setupElasticsearchClient() throws UnknownHostException {
        Settings settings = settingsBuilder()
                .put("cluster.name", propertyHolder.getProperty("elasticsearch.cluster.name")).build();
        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(
                        getByName(propertyHolder.getProperty("elasticsearch.host")), 9300));

  /*      setProperty("es.path.home", propertyHolder.getProperty("es.path.home"));
        client = NodeBuilder.nodeBuilder().settings(settings).client(true).node().client();
*/
        autoCreateBlogIndexMapping();
    }

    private void autoCreateBlogIndexMapping() {
        try {
            String esIndex = "de/bischinger/jallablog/blogmapping.json";
            String source = IOUtils.toString(currentThread().getContextClassLoader()
                    .getResourceAsStream(esIndex));
            CreateIndexRequestBuilder cirb = this.client.admin().indices().prepareCreate(indexName)
                    .addMapping("blog", source);
            cirb.execute().actionGet();
            getLogger(this.getClass()).info("blog index created");
        } catch (Exception e) {
            getLogger(this.getClass()).info("blog index already existent");
        }
    }

    private void autoCreateRemoteBlogIndexMapping() {
        try {
            String esIndex = "de/bischinger/jallablog/remoteblogmapping.json";
            String source = IOUtils.toString(currentThread().getContextClassLoader()
                    .getResourceAsStream(esIndex));
            CreateIndexRequestBuilder cirb = this.client.admin().indices().prepareCreate(indexName)
                    .addMapping("blog", source);
            cirb.execute().actionGet();
            getLogger(this.getClass()).info("blog index created");
        } catch (Exception e) {
            getLogger(this.getClass()).info("blog index already existent");
        }
    }

    //curl -XPUT http://127.0.0.1:8080/blogs/1 -d '{"name":    "John Smith","comment":    "comment"}'
    private void handleAddBlog(RoutingContext routingContext) {
        String blogKey = routingContext.request().getParam("name");
        HttpServerResponse response = routingContext.response();
        if (blogKey == null) {
            sendError(400, response);
        } else {
            JsonObject blog = routingContext.getBodyAsJson();
            if (blog == null) {
                sendError(400, response);
            } else {
                //Map to name to id
                String id = blog.getString("name");

                vertx.executeBlocking(future -> {
                    //Only index if not already present
                    GetResponse getId = client.get(getRequest(indexName).id(id)).actionGet();
                    boolean idExists = getId.isExists();
                    if (idExists) {
                        response.setStatusCode(204);
                    } else {
                        client.index(indexRequest(indexName).type(typeName).id(id).source(blog.toString())).actionGet();
                        getLogger(getClass()).info("Added object with id: " + id);
                        response.setStatusCode(201).putHeader("Location", "http://127.0.0.1:8080/blogs/"+id);
                    }
                    future.complete();
                }, res -> response.end());
            }
        }
    }

    //curl http://localhost:8080/blogs
    private void handleListBlogs(RoutingContext routingContext) {
        JsonArray arr = new JsonArray();

        vertx.executeBlocking(future -> {
            future.complete(client.prepareSearch(indexName).execute().actionGet());
        }, result -> {
            SearchResponse searchResponse = (SearchResponse) result.result();
            searchResponse.getHits().forEach(hit -> arr.add(hit.getSourceAsString()));
            routingContext.response().putHeader("content-type", "application/json").end(arr.encodePrettily());
        });
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }
}