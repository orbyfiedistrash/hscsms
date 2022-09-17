package net.orbyfied.hscsms.net;

import net.orbyfied.hscsms.net.handler.HandlerNode;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkHandler {

    private static final Logger LOGGER = Logging.getLogger("NetworkHandler");

    // the network manager
    final NetworkManager manager;

    // the socket
    Socket socket;
    // the data streams
    DataInputStream inputStream;
    DataOutputStream outputStream;

    // the handler node
    HandlerNode node = new HandlerNode(null);

    // worker
    AtomicBoolean active = new AtomicBoolean(true);
    WorkerThread workerThread = new WorkerThread();

    public NetworkHandler(final NetworkManager manager) {
        this.manager = manager;
    }

    /**
     * Get the network handler node.
     * @return The top node.
     */
    public HandlerNode node() {
        return node;
    }

    public NetworkHandler fatalClose() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }

    public NetworkManager getManager() {
        return manager;
    }

    public NetworkHandler connect(Socket socket) {
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

    public NetworkHandler sendSync(Packet packet) {
        try {
            // write packet type
            outputStream.writeInt(packet.type.id.hashCode());

            // serialize packet
            packet.type.serializer.serialize(packet.type, packet, outputStream);

            // flush
            outputStream.flush();

            // return
            return this;
        } catch (Throwable t) {
            t.printStackTrace();
            return this;
        }
    }

    public NetworkHandler start() {
        active.set(true);
        workerThread.start();
        return this;
    }

    public NetworkHandler stop() {
        active.set(false);
        return this;
    }

    public boolean isActive() {
        return active.get();
    }

    public Socket getSocket() {
        return socket;
    }

    /* ---- Worker ---- */

    class WorkerThread extends Thread {
        static int id = 0;

        public WorkerThread() {
            super("NHWorker-" + (id++));
        }

        @Override
        public void run() {
            try {
                // main network loop
                while (!socket.isClosed() && active.get()) {
                    // listen for incoming packets
                    int packetTypeId = inputStream.readInt();
                    // get packet type
                    PacketType<? extends Packet> packetType =
                            manager.getByHash(packetTypeId);
                    // handle packet
                    if (packetType != null) {
                        // deserialize
                        Packet packet = packetType.deserializer
                                .deserialize(packetType, inputStream);
                        // handle
                        NetworkHandler.this.node()
                                .handle(NetworkHandler.this, packet);
                    }
                }

                // make sure its set inactive
                active.set(false);
            } catch (Throwable t) {
                fatalClose();
                LOGGER.err(this.getName() + ": Error in worker network loop");
                t.printStackTrace();
            }
        }
    }

}
