package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.common.protocol.PacketClientboundDisconnect;
import net.orbyfied.hscsms.common.protocol.PacketClientboundPublicKey;
import net.orbyfied.hscsms.network.handler.ChainAction;
import net.orbyfied.hscsms.network.handler.HandlerNode;
import net.orbyfied.hscsms.network.handler.NodeAction;
import net.orbyfied.hscsms.network.handler.SocketNetworkHandler;
import net.orbyfied.hscsms.server.client.DisconnectReason;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;
import net.orbyfied.j8.util.logging.formatting.TextFormat;

import java.net.Socket;

public class ServerClient {

    public static String toStringAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    private static final Logger LOGGER = Logging.getLogger("ServerClient");

    /////////////////////////////////////////

    // the server
    final Server server;

    // the network handler
    SocketNetworkHandler networkHandler;

    // the user this client has authenticated as
    // this is null at first
    User user;

    public ServerClient(Server server, Socket socket) {
        this.server = server;
        networkHandler = new SocketNetworkHandler(
                server.networkManager(),
                server.utilityNetworkHandler()
        )
                .owned(this)
                .withDisconnectHandler(this::onDisconnect)
                .connect(socket);
    }

    // disconnect handler
    private void onDisconnect(Throwable t) {
        if (t == null) {
            LOGGER.info("{0} disconnected", this);
        } else {
            LOGGER.err("{0} disconnected with error", this);
            LOGGER.err(TextFormat.RED_FG + t.toString());
        }

        // destroy client
        this.destroy();
    }

    /**
     * Destroys this client, disconnecting it
     * and removing it from the server list.
     */
    public void destroy() {
        // disconnect client
        disconnect(DisconnectReason.DESTROY);

        // remove client
        server.clients.remove(this);
    }

    /**
     * Disconnects the client from the
     * server.
     */
    public void disconnect(DisconnectReason reason) {
        // close socket
        if (!networkHandler.getSocket().isClosed()) {
            try {
                try {
                    // send disconnect packet
                    if (reason != null) {
                        PacketClientboundDisconnect packet =
                                new PacketClientboundDisconnect(reason);
                        networkHandler.sendSync(packet);
                    }
                } catch (Exception e) {
                    LOGGER.err("Error while disconnecting {0}: Send disconnect packet", this);
                }

                // disconnect socket
                networkHandler.getSocket().close();
            } catch (Exception e) {
                LOGGER.err("Error while disconnecting {0}", this);
            }
        }
    }

    public ServerClient stop() {
        // stop network handler
        networkHandler.stop();

        // return
        return this;
    }

    public ServerClient start() {
        // start network handler
        networkHandler.start();

        // return
        return this;
    }

    public ServerClient readyTopLevelEncryption() {
        // add handshake handler to client
        networkHandler.node().childForType(null)
                .withHandler((handler, node, packet) -> {
                    // decrypt and store key

                    return new HandlerNode.Result(ChainAction.CONTINUE)
                            .nodeAction(NodeAction.REMOVE);
                });

        // send public key
        networkHandler.sendSync(new PacketClientboundPublicKey(server.topLevelEncryption.getPublicKey()));

        // return
        return this;
    }

    /* ---------- Other ------------ */

    @Override
    public String toString() {
        return "ServerClient[" + toStringAddress() + "]";
    }

    public String toStringAddress() {
        return toStringAddress(networkHandler.getSocket());
    }

}
