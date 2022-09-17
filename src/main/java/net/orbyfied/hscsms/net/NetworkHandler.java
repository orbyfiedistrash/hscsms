package net.orbyfied.hscsms.net;

import net.orbyfied.hscsms.net.handler.HandlerNode;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkHandler {

    private static final Logger LOGGER = Logging.getLogger("NetworkHandler");

    // the network manager
    final NetworkManager manager;

    // the socket
    Socket socket;

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
     * @return
     */
    public HandlerNode node() {
        return node;
    }

    public NetworkManager getManager() {
        return manager;
    }

    public NetworkHandler connect(Socket socket) {
        this.socket = socket;
        return this;
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
                // get streams
                DataInputStream inputStream =
                        new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream outputStream =
                        new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

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
                LOGGER.err(this.getName() + ": Error in worker");
                t.printStackTrace();
            }
        }
    }

}
