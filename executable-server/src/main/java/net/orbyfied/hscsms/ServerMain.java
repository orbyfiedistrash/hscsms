package net.orbyfied.hscsms;

import net.orbyfied.hscsms.core.resource.ServerResourceHandle;
import net.orbyfied.hscsms.libexec.ArgParseException;
import net.orbyfied.hscsms.libexec.ArgParser;
import net.orbyfied.hscsms.libexec.YamlConfig;
import net.orbyfied.hscsms.server.Server;
import net.orbyfied.hscsms.server.resource.User;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;

public class ServerMain {

    static {
        Logging.setFormatted(true);
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
        Values argVals;
        try {
            argVals = new ArgParser()
                    .withArgument("work-dir", Path.class)
                    .parseConsoleArgs(args);
        } catch (ArgParseException e) {
            System.err.println("Error ArgParser: " + e.getMessage());
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            return;
        }

        // set properties
        workingDir = argVals.getOrDefault("work-dir", workingDir);

        // create server
        server = new Server();

        // load configuration
        LOGGER.info("Loading configuration from file");
        Values configValues = YamlConfig.copyDefaultsAndLoad(workingDir.resolve("config.yml"), ServerMain.class, "/config-defaults.yml");
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

        // await a worker to block until the server stops
        server.serverSocketWorker.await();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
