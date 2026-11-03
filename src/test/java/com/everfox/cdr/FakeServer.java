/*
 * Copyright 2026 Everfox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.everfox.cdr;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A local HTTP server for client unit tests. It records every request, and replies according to routes registered by
 * path prefix.
 *
 * <p>A route can have several replies, which are sent in order, and the last is repeated once the others are used. The
 * route with the longest matching prefix is used, and a request no route matches gets a 404.
 */
public final class FakeServer implements AutoCloseable {

    /**
     * A request received by the server.
     *
     * @param method the HTTP method
     * @param path the request path
     * @param headers the request headers
     * @param body the request body
     */
    public record Request(String method, String path, Headers headers, byte[] body) {}

    /**
     * A reply for the server to send.
     *
     * @param status the HTTP status
     * @param body the response body, which is not sent if empty
     * @param headers the response headers
     */
    public record Reply(int status, byte[] body, Map<String, String> headers) {

        /**
         * Creates a reply without headers.
         *
         * @param status the HTTP status
         * @param body the response body as UTF-8 text
         * @return the reply
         */
        public static Reply of(int status, String body) {
            return new Reply(status, body.getBytes(StandardCharsets.UTF_8), Map.of());
        }

        /**
         * Returns a copy of this reply with an added header.
         *
         * @param name the header name
         * @param value the header value
         * @return the new reply
         */
        public Reply withHeader(String name, String value) {
            Map<String, String> newHeaders = new LinkedHashMap<>(headers);
            newHeaders.put(name, value);
            return new Reply(status, body, newHeaders);
        }
    }

    /** The underlying server. */
    private final HttpServer server;
    /** Requests received, in order. */
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    /** Replies by path prefix. */
    private final Map<String, Deque<Reply>> routes = new LinkedHashMap<>();

    /**
     * Creates the server.
     *
     * @param server the underlying server
     */
    private FakeServer(HttpServer server) {
        this.server = server;
    }

    /**
     * Starts a server on a free loopback port.
     *
     * @return the server
     * @throws IOException if the server cannot be started
     */
    public static FakeServer start() throws IOException {
        FakeServer fake =
                new FakeServer(HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0));
        fake.server.createContext("/", fake::handle);
        fake.server.start();
        return fake;
    }

    /**
     * Returns the server's base URL, for example {@code http://127.0.0.1:54321}.
     *
     * @return the base URL
     */
    public URI baseUrl() {
        return URI.create("http://" + server.getAddress().getHostString() + ":"
                + server.getAddress().getPort());
    }

    /**
     * Sets the replies for requests whose path starts with a prefix, replacing any previous replies for it.
     *
     * @param pathPrefix the path prefix
     * @param replies the replies, sent in order; the last is repeated
     * @return this server
     */
    public FakeServer route(String pathPrefix, Reply... replies) {
        if (replies.length == 0) {
            throw new IllegalArgumentException("A route needs at least one reply");
        }
        synchronized (routes) {
            routes.put(pathPrefix, new ArrayDeque<>(List.of(replies)));
        }
        return this;
    }

    /**
     * Returns the requests received, in order.
     *
     * @return the requests
     */
    public List<Request> requests() {
        return List.copyOf(requests);
    }

    /**
     * Returns the requests received whose path starts with a prefix, in order.
     *
     * @param pathPrefix the path prefix
     * @return the matching requests
     */
    public List<Request> requests(String pathPrefix) {
        return requests.stream().filter(r -> r.path().startsWith(pathPrefix)).toList();
    }

    /**
     * Returns the most recent request.
     *
     * @return the request
     * @throws IllegalStateException if no request has been received
     */
    public Request lastRequest() {
        if (requests.isEmpty()) {
            throw new IllegalStateException("No request received");
        }
        return requests.get(requests.size() - 1);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Records a request and sends the reply for it.
     *
     * @param exchange the exchange
     * @throws IOException if the reply cannot be sent
     */
    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        requests.add(new Request(
                exchange.getRequestMethod(),
                path,
                exchange.getRequestHeaders(),
                exchange.getRequestBody().readAllBytes()));
        Reply reply = nextReply(path);
        reply.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(reply.status(), reply.body().length == 0 ? -1 : reply.body().length);
        if (reply.body().length > 0) {
            exchange.getResponseBody().write(reply.body());
        }
        exchange.close();
    }

    /**
     * Returns the next reply for a path from the route with the longest matching prefix.
     *
     * @param path the request path
     * @return the reply, or a 404 if no route matches
     */
    private Reply nextReply(String path) {
        synchronized (routes) {
            String best = null;
            for (String prefix : routes.keySet()) {
                if (path.startsWith(prefix) && (best == null || prefix.length() > best.length())) {
                    best = prefix;
                }
            }
            if (best == null) {
                return Reply.of(404, "");
            }
            Deque<Reply> replies = routes.get(best);
            return replies.size() > 1 ? replies.poll() : replies.peek();
        }
    }
}
