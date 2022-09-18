package net.orbyfied.hscsms.network.handler;

import net.orbyfied.hscsms.network.NetworkHandler;
import net.orbyfied.hscsms.network.NetworkManager;
import net.orbyfied.hscsms.network.Packet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Network handler which purpose is solely
 * to handle packets and connections from
 * other handlers which delegate to it.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class UtilityNetworkHandler extends NetworkHandler<UtilityNetworkHandler> {

    // the queue of async tasks
    Deque<Runnable> tasks = new ArrayDeque<>();
    // the waiting system
    Object lock = new Object();
    AtomicBoolean waiting = new AtomicBoolean(false);

    public UtilityNetworkHandler(NetworkManager manager, NetworkHandler parent) {
        super(manager, parent);
    }

    @Override
    protected WorkerThread createWorkerThread() {
        return new UtilityWorkerThread();
    }

    @Override
    protected void handle(Packet packet) {
        super.handle(packet);

        // call handler node
        node.handle(this, packet);
    }

    @Override
    protected boolean canHandleAsync(Packet packet) {
        return true;
    }

    @Override
    protected void scheduleHandleAsync(final Packet packet) {
        // add task
        schedule(() -> handle(packet));
    }

    /*
        Tasks
     */

    public UtilityNetworkHandler schedule(Runnable runnable) {
        synchronized (tasks) {
            tasks.add(runnable);
        }

        if (waiting.get()) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        return this;
    }

    /* ---------- Worker ---------- */

    class UtilityWorkerThread extends WorkerThread {

        @Override
        public void runSafe() throws Throwable {
            // main loop
            while (active.get()) {
                // execute tasks
                synchronized (tasks) {
                    while (!tasks.isEmpty()) {
                        // poll new task
                        Runnable task = tasks.poll();

                        // execute
                        task.run();
                    }
                }

                // wait for tasks
                synchronized (lock) {
                    waiting.set(true);
                    lock.wait();
                    waiting.set(false);
                }
            }
        }

    }

}
