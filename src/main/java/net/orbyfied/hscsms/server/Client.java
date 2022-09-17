package net.orbyfied.hscsms.server;

import net.orbyfied.hscsms.net.NetworkHandler;

import javax.crypto.Cipher;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

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

    // the top layer RSA encryption keys
    KeyPair tlKeyPair;

    // the user this client has authenticated as
    // this is null at first
    User user;

    public Client(Server server, Socket socket) {
        this.server = server;
        networkHandler = new NetworkHandler(server.networkManager())
                .connect(socket);
    }

    public Client readyTopLevelEncryption() {
        try {
            // generate keys
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            tlKeyPair = generator.generateKeyPair();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // add handshake handler to client

        // return
        return this;
    }

}
