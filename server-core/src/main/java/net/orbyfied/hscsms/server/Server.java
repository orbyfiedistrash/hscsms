package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.common.ProtocolSpec;
import net.orbyfied.hscsms.core.ServiceManager;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.handler.UtilityNetworkHandler;
import net.orbyfied.hscsms.security.EncryptionProfile;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    /* ------ Core ------ */

    // if the server should be running
    AtomicBoolean active = new AtomicBoolean(false);

    // the logger
    Logger logger = Logging.getLogger("Server");

    /* ------ Networking ------ */

    // the servers socket address
    SocketAddress address;
    // the server socket
    ServerSocket socket;

    // the server utility network handler
    UtilityNetworkHandler networkHandler;

    public UtilityNetworkHandler utilityNetworkHandler() {
        return networkHandler;
    }

    // the currently connected clients
    List<ServerClient> clients = new ArrayList<>();

    /* ------ Security ----- */

    // the top level encryption
    public final EncryptionProfile topLevelEncryption
            = new EncryptionProfile("RSA", "ECB", "PKCS1Padding", 1024);

    /* ------ Top-Level Services ------ */

    // the service manager
    ServiceManager serviceManager = new ServiceManager();
    // the network manager
    NetworkManager networkManager = new NetworkManager();

    public Server() { }

    /**
     * Bind and open the server on the provided
     * socket address.
     * @param address The socket address.
     * @return This.
     */
    public Server open(SocketAddress address) {
        // set address
        this.address = address;

        try {
            // create and bind socket
            socket = new ServerSocket();
            socket.bind(address);

            logger.ok("Connected server on {0}", address);
        } catch (Exception e) {
            logger.err("Failed to connect server on {0}", address);
            e.printStackTrace();
        }

        try {
            // create utility network handler
            networkHandler = new UtilityNetworkHandler(networkManager, null)
                    .owned(this);

            // start
            networkHandler.start();

            logger.ok("Started utility network handler");
        } catch (Exception e) {
            logger.err("Failed to start utility network handler");
            e.printStackTrace();
        }

        try {
            // load protocol spec
            ProtocolSpec.loadProtocol(networkManager);

            logger.ok("Loaded protocol");
        } catch (Exception e) {
            logger.err("Failed to load protocol");
            e.printStackTrace();
        }

        // generate top level key pair
        try {
            topLevelEncryption.generateKeyPair(1024);
            logger.ok("Generated top level RSA key pair");
        } catch (Exception e) {
            logger.err("Failed to generate top level RSA key pair");
            e.printStackTrace();
        }

        // return
        return this;
    }

    public Server setActive(boolean b) {
        active.set(b);
        return this;
    }

    public boolean isActive() {
        return active.get();
    }

    /**
     * The main server network loop.
     */
    public void runMain() {
        // while running
        while (active.get()) {
            // check if the socket is still open
            if (socket.isClosed()) {
                // report and close server
                logger.info("Socket closed, shutting down");
                shutdownProcess();
            }

            try {
                // accept connection (blocking)
                Socket clientSocket = socket.accept();

                // add client
                try {
                    ServerClient client = new ServerClient(this, clientSocket);
                    clients.add(client);
                    client.start();

                    logger.info("Accepted and started {0}", client);
                } catch (Exception e) {
                    logger.err("Error while accepting connection from [{0}]",
                            ServerClient.toStringAddress(clientSocket));
                }
            } catch (Exception e) {
                logger.err("Error while accepting connections");
                e.printStackTrace();
            }
        }

        // close logger group
        Logging.getGroup().setActive(false);
    }

    private void shutdownProcess() {
        logger.info("Shutting down...");
    }

    /* ------ Top-Level Services ------ */

    /**
     * Get the core service manager.
     * @return The service manager.
     */
    public ServiceManager services() {
        return serviceManager;
    }

    /**
     * Get the core network manager.
     * @return The network manager.
     */
    public NetworkManager networkManager() {
        return networkManager;
    }

}
