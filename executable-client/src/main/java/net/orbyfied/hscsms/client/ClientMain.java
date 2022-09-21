package net.orbyfied.hscsms.client;

import net.orbyfied.hscsms.client.app.ClientApp;
import net.orbyfied.hscsms.client.app.ConnectScreenAC;
import net.orbyfied.hscsms.client.applib.impl.ExitAC;
import net.orbyfied.hscsms.common.protocol.PacketServerboundDisconnect;
import net.orbyfied.hscsms.libexec.ArgParseException;
import net.orbyfied.hscsms.libexec.ArgParser;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.server.ServerClient;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Scanner;

public class ClientMain {

    public static final Logger LOGGER
            = Logging.getLogger("Client");

    ///////////////////////////////////////

    ClientMain() {

    }

    /**
     * Client network manager.
     */
    public final NetworkManager networkManager =
            new NetworkManager();

    /**
     * Client network handler.
     */
    public final SocketNetworkHandler networkHandler =
             new SocketNetworkHandler(networkManager, null);

    /**
     * The working directory.
     */
    public Path workDir;

    /**
     * The client application.
     */
    public ClientApp app;

    public void disconnect() {
        if (networkHandler.isOpen()) {
            networkHandler.sendSync(new PacketServerboundDisconnect());
            networkHandler.disconnect();
            networkHandler.stop();
        }
    }

    public void reconnect(Socket socket) {
        LOGGER.info("Reconnecting to [" + ServerClient.toStringAddress(socket) + "]");
        disconnect();
        networkHandler.connect(socket);
        networkHandler.start();
    }

    public void reconnect(InetSocketAddress address) {
        try {
            Socket socket = new Socket();
            socket.connect(address);
            reconnect(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------------------------- */

    private static ClientMain client;

    public static ClientMain getClient() {
        return client;
    }

    public void run(String[] args) {
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
        workDir = argVals.getOrDefault("work-dir", Path.of("./hscsms-client"));

        // create application
        app = new ClientApp(this);

        {
            app.appContextManager.addContext(ExitAC::new);
            app.appContextManager.addContext(ConnectScreenAC::new);
        }

        app.appContextManager.setCurrentContext("connect_screen");

        // prepare input
        Scanner scanner = new Scanner(System.in);

        // input address
        String[] addr = scanner.nextLine().split(":");
        String host = addr[0];
        int port = 42069;
        if (addr.length > 1)
            port = Integer.parseInt(addr[1]);

        // connect to server
        reconnect(new InetSocketAddress(host, port));
        System.out.println();

        // while socket is open
        Socket socket = networkHandler.getSocket();
        while (!socket.isClosed()) {
            // get line
            String line = scanner.nextLine();

            // client command
            if (line.startsWith("/")) {
                // get command
                String cmdStr = line.substring(1);
                if ("q".equals(cmdStr)) {
                    disconnect();
                    break;
                }
            } else {
                // send message to server

            }
        }

        // disconnect socket
        try {
            if (networkHandler.active()) {
                networkHandler.disconnect();
                networkHandler.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // create client
        client = new ClientMain();
        client.run(args);
    }

}
