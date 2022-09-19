package net.orbyfied.hscsms.network;

import net.orbyfied.hscsms.network.handler.HandlerNode;
import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic network handler.
 * @param <S> Self.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class NetworkHandler<S extends NetworkHandler> {

    protected static final Logger LOGGER = Logging.getLogger("NetworkHandler");

    /**
     * The owner of this handler.
     */
    protected Object owner;

    // the network manager
    protected final NetworkManager manager;
    // the optional parent
    protected final NetworkHandler parent;

    // the handler node
    protected HandlerNode node = new HandlerNode(null);

    // worker
    protected AtomicBoolean active = new AtomicBoolean(true);
    protected WorkerThread workerThread;

    private final S self;

    public NetworkHandler(final NetworkManager manager,
                          final NetworkHandler parent) {
        this.manager = manager;
        this.parent  = parent;
        this.self    = (S) this;
    }

    public S owned(Object owner) {
        this.owner = owner;
        return self;
    }

    /**
     * Get the network handler node.
     * @return The top node.
     */
    public HandlerNode node() {
        return node;
    }

    public NetworkManager manager() {
        return manager;
    }

    public S start() {
        // create worker thread
        if (workerThread == null)
            workerThread = createWorkerThread();
        // quit if still null
        if (workerThread == null)
            return self;

        // set active
        active.set(true);
        workerThread.start();
        return self;
    }

    public S stop() {
        active.set(false);
        return self;
    }

    public S fatalClose() {
        // doesnt do anything by default
        return self;
    }

    public boolean active() {
        return active.get();
    }

    protected abstract WorkerThread createWorkerThread();

    /**
     * Handles an incoming packet.
     * @param packet The packet.
     */
    protected void handle(Packet packet) {
        // call parent
        if (parent != null)
            parent.handle(packet);
    }

    protected abstract boolean canHandleAsync(Packet packet);
    protected abstract void scheduleHandleAsync(Packet packet);

    /* ---- Worker ---- */

    public abstract class WorkerThread extends Thread {
        static int id = 0;

        public WorkerThread() {
            super("NHWorker-" + (id++));
        }

        @Override
        public void run() {
            try {
                runSafe();
            } catch (Throwable t) {
                fatalClose();
                LOGGER.err(this.getName() + ": Error in socket worker network loop");
                t.printStackTrace(Logging.ERR);
            }

            // make sure inactive status
            active.set(false);
        }

        /**
         * Should run actual code.
         * Abstracts away the error handling.
         */
        public abstract void runSafe() throws Throwable;
    }

}
