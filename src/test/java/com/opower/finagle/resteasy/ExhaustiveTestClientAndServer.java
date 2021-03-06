package com.opower.finagle.resteasy;

import com.opower.finagle.resteasy.client.ResteasyClientBuilder;
import com.opower.finagle.resteasy.server.ResteasyServiceBuilder;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;
import com.twitter.util.Duration;
import org.jboss.netty.channel.ChannelException;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Creates a client and a server and sends data back and forth.
 *
 * @author ed.peters
 */
public class ExhaustiveTestClientAndServer {

    /*
     * First port number to start checking when trying to open up a
     * server for testing
     */
    public static final int BASE_PORT = 10000;

    /**
     * Maximum number of attempts to create a server before the test
     * will be considered a failure
     */
    public static final int MAX_TRIES = 10;

    private ExampleServiceImpl impl;
    private Service service;
    private Server server;
    private ExampleService client;
    private int port;

    @Test
    public void performExhaustiveTest() {

        this.impl = new ExampleServiceImpl();
        this.service = ResteasyServiceBuilder.get()
            .withEndpoint(impl)
            .build();

        this.port = BASE_PORT;
        while (this.port < BASE_PORT + MAX_TRIES) {
            this.server = createServer(this.service, this.port);
            if (this.server != null) {
                break;
            }
        }
        assertNotNull("couldn't allocate server", server);

        this.client = ResteasyClientBuilder.get()
                .withHttpClient("localhost", this.port)
                .build(ExampleService.class);

        for (int i = 0; i < 10; i++) {
            String [] expectedValue = new String [] {
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            };
            client.setBar(expectedValue);
            assertArrayEquals("wrong value passed",
                    expectedValue,
                    impl.getBar());
            assertArrayEquals("wrong value returned",
                    expectedValue,
                    client.getBar());
        }

        server.close(Duration.zero());
    }

    /*
     * Opens up a new server on an available port by starting at the
     * BASE_PORT and going up from there.  If this doesn't succeed
     * after MAX_TRIES, throws an exception.
     */
    protected static Server createServer(Service service, int port) {
        try {
            ServerBuilder builder = ServerBuilder
                    .get()
                    .sendBufferSize(256)
                    .codec(Http.get())
                    .name("HttpServer")
                    .bindTo(new InetSocketAddress("localhost", port));
            return ServerBuilder.safeBuild(service, builder);
        }
        catch (ChannelException e) {
            if (e.getCause() instanceof BindException) {
                return null;
            }
            throw e;
        }
    }


    /**
     * Simple service interface for the test -- data comes in,
     * data goes out
     */
    @Path("/foo")
    public interface ExampleService {

        @GET
        @Path("/bar")
        @Produces(MediaType.APPLICATION_JSON)
        String [] getBar();

        @POST
        @Path("/bar")
        @Consumes(MediaType.APPLICATION_JSON)
        void setBar(String [] bar);

    }

    /**
     * Implementation of the service
     */
    public static class ExampleServiceImpl implements ExampleService {

        private String [] bar;

        @Override
        public String [] getBar() {
            return this.bar;
        }

        @Override
        public void setBar(String [] bar) {
            this.bar = bar;
        }

    }

}
