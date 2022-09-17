package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.common.protocol.PacketClientboundPublicKey;
import net.orbyfied.hscsms.net.NetworkHandler;
import net.orbyfied.hscsms.net.handler.HandlerNode;

import javax.crypto.Cipher;
import java.net.Socket;

public class Client {

    private static Cipher CIPHER;

    static {
        try {
            CIPHER = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////

    // the server
    final Server server;

    // the network handler
    NetworkHandler networkHandler;

    // the user this client has authenticated as
    // this is null at first
    User user;

    public Client(Server server, Socket socket) {
        this.server = server;
        networkHandler = new NetworkHandler(server.networkManager())
                .connect(socket);
    }

    public Client readyTopLevelEncryption() {
        // add handshake handler to client
        networkHandler.node().childForType(null)
                .withHandler((handler, node, packet) -> {
                    // decrypt and store key

                    return new HandlerNode.Result(HandlerNode.Chain.CONTINUE)
                            .nodeAction(HandlerNode.NodeAction.REMOVE);
                });

        // send public key
        networkHandler.sendSync(new PacketClientboundPublicKey(server.topLevelEncryption.getPublicKey()));

        // return
        return this;
    }

}
