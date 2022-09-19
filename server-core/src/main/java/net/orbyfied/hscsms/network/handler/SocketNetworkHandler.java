package net.orbyfied.hscsms.network.handler;

import net.orbyfied.hscsms.network.NetworkHandler;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            outputStream.writeInt(packet.type().identifier().hashCode());

            // serialize packet
            packet.type().serializer().serialize(packet.type(), packet, outputStream);

            // flush
            outputStream.flush();

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace();
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
                    int packetTypeId = inputStream.readInt();
                    // get packet type
                    PacketType<? extends Packet> packetType =
                            manager.getByHash(packetTypeId);

                    // handle packet
                    if (packetType != null) {
                        // increment packet count
                        pC++;

                        // deserialize
                        Packet packet = packetType.deserializer()
                                .deserialize(packetType, inputStream);
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
