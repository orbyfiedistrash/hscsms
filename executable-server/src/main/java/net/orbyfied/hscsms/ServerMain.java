package net.orbyfied.hscsms;

import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.server.resource.ServerChannelMessage;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ServerMain {

    static {
        Logging.setFormatted(false);
    }

    // server main logger
    public static final Logger LOGGER = Logging.getLogger("ServerMain");

    // server instance
    public static Server server;
    // the working directory
    public static Path workingDir = Path.of(".");

    /* --------------------------- */

    public static void main(String[] args) {
        // parse args
        Map<String, Object> argVals = new HashMap<>();
        String key = null;
        for (int i = 0; i < args.length; i++) {
            String str = args[i];
            // parse key
            if (str.startsWith("--")) {
                key = str.substring(2);
                continue;
            }

            // parse value
            Object val;
            switch (key) {
                case "work-dir" -> {
                    val = Path.of(str);
                }

                default -> {
                    LOGGER.err("Unknown arg key: " + key);
                    return;
                }
            }
            argVals.put(key, val);
        }

        // set properties
        workingDir = (Path) argVals.getOrDefault("work-dir", workingDir);

        // create server
        server = new Server();

        // load configuration
        LOGGER.info("Loading configuration from file");
        Values configValues = ServerYamlConfig.copyDefaultsAndLoad(workingDir.resolve("config.yml"), ServerMain.class, "/config-defaults.yml");
        if (configValues == null) {
            LOGGER.err("Failed to load configuration, exiting");
            return;
        }

        server.configuration.putAll(configValues);

        // open connection
        server.open(new InetSocketAddress(configValues.getOrDefault("port", 42069)));

        // run server on main thread
        server
                .setActive(true)
                .setup()
                .start();

        // resource test
        ServerResourceManager resourceManager = server.resourceManager();

        {
            ServerChannelMessage message = resourceManager.createResource(ServerChannelMessage.TYPE);
            UUID uuid = message.universalID();
            message.setContentRaw("hello guys");
            System.out.println("CREATED /// " + message);
            System.out.println(message.getContentRaw());
            resourceManager.saveResourceAsync(message).join();

            resourceManager.loadResourceAsync(uuid).whenComplete((message1, t) -> {
                if (t != null) {
                    t.printStackTrace(Logging.ERR);
                    return;
                }

                System.out.println("LOADED /// " + message1);
                System.out.println(message.getContentRaw());
            });
        }

        // await a worker to block until the server stops
        server.serverSocketWorker.await();
    }

}
