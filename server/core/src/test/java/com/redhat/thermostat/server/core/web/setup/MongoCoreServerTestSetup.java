package com.redhat.thermostat.server.core.web.setup;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.client.HttpClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.redhat.thermostat.server.core.web.CoreServer;
import com.redhat.thermostat.test.util.MongodTestUtil;

public class MongoCoreServerTestSetup {
    private static CoreServer coreServer;
    protected static HttpClient client;
    private static int port;
    protected final String baseUrl = "http://localhost:" + port + "/api/v100";
    private static final MongodTestUtil mongodTestUtil = new MongodTestUtil();


    private static Thread thread;
    private static final AtomicBoolean ready = new AtomicBoolean(false);

    @BeforeClass
    public static void setupClass() throws Exception {
        ready.getAndSet(false);

        mongodTestUtil.startMongod();
        mongodTestUtil.waitForMongodStart();

        coreServer= new CoreServer();
        coreServer.buildServer(Collections.EMPTY_MAP, MongodTestUtil.mongoConfiguration, Collections.EMPTY_MAP);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    coreServer.getServer().start();
                    ready.getAndSet(true);
                    coreServer.getServer().join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        client = new HttpClient();
        client.start();

        port = coreServer.getPort();

    }

    @AfterClass
    public static void cleanupClass() throws Exception {
        coreServer.finish();
        thread.join();

        mongodTestUtil.stopMongod();
        mongodTestUtil.waitForMongodStop();
        mongodTestUtil.finish();
    }

    @Before
    public void setup() throws InterruptedException {
        while (!ready.get()){
            Thread.sleep(100L);
        }
    }
}
