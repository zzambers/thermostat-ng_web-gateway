package com.redhat.thermostat.server.core;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import com.redhat.thermostat.server.core.internal.security.auth.proxy.ProxyAuthFilter;
import com.redhat.thermostat.server.core.internal.storage.MongoStorage;
import com.redhat.thermostat.server.core.internal.web.handler.http.HttpHandler;
import com.redhat.thermostat.server.core.internal.web.handler.storage.MongoStorageHandler;

@Component
@Service(CoreServer.class)
public class CoreServer {

    private Server server;

    public void buildServer(Properties properties) {
        MongoStorage.start("thermostat", 27518);

        URI baseUri = UriBuilder.fromUri("http://localhost").port(8080).build();

        ResourceConfig config = new ResourceConfig();
        setupResourceConfig(config);

        server = JettyHttpContainerFactory.createServer(baseUri, config, false);


        ServerConnector httpConnector = new ServerConnector(server);
        setupConnector(httpConnector);
        server.setConnectors(new Connector[]{httpConnector});


        HandlerList handlers = new HandlerList();
        setupHandlers(handlers);

        server.setHandler(handlers);
    }

    private void setupResourceConfig(ResourceConfig config) {
        config.register(new HttpHandler(new MongoStorageHandler()));
        config.register(new ProxyAuthFilter());
        config.register(new RolesAllowedDynamicFeature());
    }

    private void setupConnector(ServerConnector httpConnector) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());

        httpConnector.addConnectionFactory(new HttpConnectionFactory(httpConfig));

        httpConnector.setHost("localhost");
        httpConnector.setPort(8090);
        httpConnector.setIdleTimeout(30000);
    }

    private void setupHandlers(HandlerList handlers) {
        Handler originalHandler = server.getHandler();
        handlers.setHandlers(new Handler[] { createSwaggerResource(), originalHandler});
    }

    private Handler createSwaggerResource() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{ "index.html" });
        resourceHandler.setResourceBase("");
        URL u = this.getClass().getResource("/swagger/index.html");
        URI root;
        try {
            root = u.toURI().resolve("./").normalize();
            resourceHandler.setBaseResource(Resource.newResource(root));
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }
        return resourceHandler;
    }

    public Server getServer() {
        return server;
    }

    public void finish() {
        MongoStorage.finish();
    }
}
