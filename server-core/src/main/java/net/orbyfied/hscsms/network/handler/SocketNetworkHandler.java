package net.orbyfied.hscsms.network.handler;

import net.orbyfied.hscsms.network.NetworkHandler;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.hscsms.security.EncryptionProfile;
import net.orbyfied.hscsms.security.LegacyEncryptionProfile;
import net.orbyfied.hscsms.service.Logging;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Network handler for socket connections.
 * Bound to a socket will read, write and handle packets.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SocketNetworkHandler extends NetworkHandler<SocketNetworkHandler> {

    // the socket
    Socket socket;
    // the data streams
    DataInputStream inputStream;
    DataOutputStream outputStream;

    // disconnect handler
    Consumer<Throwable> disconnectHandler;

    // decryption profile
    EncryptionProfile decryptionProfile;

    public SocketNetworkHandler(final NetworkManager manager,
                                final NetworkHandler parent) {
        super(manager, parent);
    }

    @Override
    protected void handle(Packet packet) {
        super.handle(packet);

        // call handler node
        this.node().handle(this, packet);
    }

    public SocketNetworkHandler withDisconnectHandler(Consumer<Throwable> consumer) {
        this.disconnectHandler = consumer;
        return this;
    }

    public synchronized SocketNetworkHandler withDecryptionProfile(EncryptionProfile profile) {
        this.decryptionProfile = profile;
        return this;
    }

    @Override
    protected boolean canHandleAsync(Packet packet) {
        return false;
    }

    @Override
    protected void scheduleHandleAsync(Packet packet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketNetworkHandler fatalClose() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Throwable e) {
            e.printStackTrace(Logging.ERR);
        }

        return this;
    }

    public SocketNetworkHandler connect(Socket socket) {
        this.socket = socket;

        try {
            inputStream  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (Exception e) {
            fatalClose();
            LOGGER.err("Error while connecting");
            e.printStackTrace(Logging.ERR);
        }

        return this;
    }

    public SocketNetworkHandler disconnect() {
        // deactivate worker
        stop();

        // return
        return this;
    }

    public SocketNetworkHandler sendSync(Packet packet) {
        try {
            // write packet type
            outputStream.writeByte(/* unencrypted */ 0);
            outputStream.writeInt(packet.type().identifier().hashCode());

            // serialize packet
            packet.type().serializer().serialize(packet.type(), packet, outputStream);

            // flush
            outputStream.flush();

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace(Logging.ERR);
            return this;
        }
    }

    public SocketNetworkHandler sendSyncEncrypted(Packet packet, EncryptionProfile encryption) {
        try {
            // create output stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream stream    = encryption.encryptingOutputStream(baos).toDataStream();

            // serialize packet
            packet.type().serializer().serialize(packet.type(), packet, stream);
            stream.flush();

            // write packet type unencrypted
            outputStream.writeByte(/* encrypted */ 1);
            outputStream.writeInt(packet.type().identifier().hashCode());

            // get encrypted bytes and write
            byte[] encrypted = baos.toByteArray();
            outputStream.writeInt(encrypted.length); // write length
            outputStream.write(encrypted);

            // flush
            outputStream.flush();

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace(Logging.ERR);
            return this;
        }
    }

    public boolean isOpen() {
        if (socket == null)
            return false;
        return !socket.isClosed();
    }

    @Override
    protected NetworkHandler.WorkerThread createWorkerThread() {
        return new SocketWorkerThread();
    }

    public Socket getSocket() {
        return socket;
    }

    /* ---- Worker ---- */

    class SocketWorkerThread extends WorkerThread {

        @Override
        public void runSafe() throws Throwable {
            long pC = 0; // packet count
            Throwable t = null;

            // main network loop
            try {
                while (!socket.isClosed() && active.get()) {
                    // listen for incoming packets
                    byte encryptedFlag   = inputStream.readByte();
                    int packetTypeId = inputStream.readInt();
                    // get packet type
                    PacketType<? extends Packet> packetType =
                            manager.getByHash(packetTypeId);

//                    System.out.println("RECEIVED PACKET id-hash: " + packetTypeId + ", type: "
//                            + (packetType != null ? packetType.identifier().toString() : "null"));

                    // handle packet
                    if (packetType != null) {
                        // increment packet count
                        pC++;

                        // prepare stream
                        DataInputStream stream;
                        if (encryptedFlag == 0) {
                            // put unencrypted stream
                            stream = inputStream;
                        } else {
                            // check for decryption profile
                            if (decryptionProfile == null) {
                                throw new IllegalArgumentException("can not decrypt encrypted packet, no decryption profile set");
                            }

                            // read encrypted bytes
                            int dataLen = inputStream.readInt();
                            byte[] encrypted = inputStream.readNBytes(dataLen);

                            // create encrypted input stream
                            ByteArrayInputStream bais = new ByteArrayInputStream(encrypted);
                            stream = decryptionProfile.decryptingInputStream(bais).toDataStream();
                        }

                        // deserialize
                        Packet packet = packetType.deserializer()
                                .deserialize(packetType, stream);

                        // handle
                        SocketNetworkHandler.this.handle(packet);
                    }
                }
            } catch (Throwable t1) {
                t = t1;
            }

            // handle disconnect
            Throwable ft = null;
            if (t != null) {
                ft = t;
            }

            if (disconnectHandler != null)
                disconnectHandler.accept(ft);
        }
    }

}
