package net.orbyfied.hscsms.core.resource.impl;

import net.orbyfied.hscsms.core.resource.AbstractResourceService;
import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.hscsms.core.resource.event.ResourceHandleAcquireEvent;
import net.orbyfied.hscsms.core.resource.event.ResourceHandleReleaseEvent;
import net.orbyfied.hscsms.core.resource.event.ResourceUnloadEvent;
import net.orbyfied.hscsms.util.data.IntBox;
import net.orbyfied.hscsms.util.worker.SafeWorker;
import net.orbyfied.j8.event.handler.BasicHandler;
import net.orbyfied.j8.util.functional.ThrowableRunnable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceGCService extends AbstractResourceService {

    /**
     * Server resource type property key:
     * If set to false, the resource is not saved when
     * unloaded by the garbage collection service.
     * Default: True
     */
    public static final Object PERSISTENT = new Object();

    //////////////////////////////////////////////////////

    // if the garbage collector should automatically track
    // resource usages and act on them
    boolean automate;

    // the internal map for tracking which
    // resources are currently being used
    final ConcurrentHashMap<ServerResource, IntBox> usages = new ConcurrentHashMap<>();

    // the queue of resources to be unloaded async
    final Queue<ServerResource> queue = new ArrayDeque<>();
    // the queue worker
    final SafeWorker    worker  = new SafeWorker();
    final AtomicBoolean waiting = new AtomicBoolean(false);

    public ResourceGCService(ServerResourceManager manager) {
        super(manager);
        this.worker.withTarget(new WorkerTarget());
    }

    public SafeWorker worker() {
        return worker;
    }

    @Override
    public void added() {
        super.added();
        worker.commence();
    }

    @Override
    public void removed() {
        super.removed();
        worker.terminate();
    }

    /* ----- Manual Handling ---- */

    public void disposeImmediate(ServerResource resource) {
        // save resource if persistent
        if (resource.type().properties().getOrDefaultRaw(PERSISTENT, true)) {
            manager.saveResource(resource);
        }

        // unload resource
        manager.unloadResource(resource);
    }

    /**
     * Forces a resource to be marked acquired.
     * Use with caution, if not released manually
     * it will not be automatically disposed.
     * @param resource The resource.
     */
    public void acquireImmediate(ServerResource resource) {
        // increment usage
        usages.get(resource).value++;
    }

    /**
     * Forces a resource to be released by one.
     * @param resource The resource.
     */
    public void releaseImmediate(ServerResource resource) {
        // decrement usage
        IntBox u = usages.get(resource);
        u.value--;

        // check if we should dispose
        if (u.value <= 0)
            disposeImmediate(resource);
    }

    /* ----- Automatic Handling ----- */

    @BasicHandler
    void handleUnloaded(ResourceUnloadEvent event) {
        // remove from registries
        usages.remove(event.getResource());
    }

    @BasicHandler
    void handleReleased(ResourceHandleReleaseEvent event) {
        // check automated
        if (!automate)
            return;

        ServerResource resource = event.getHandle().getOrNull();
        if (resource == null)
            return;
        releaseImmediate(resource);
    }

    @BasicHandler
    void handleAcquired(ResourceHandleAcquireEvent event) {
        // check automated
        if (!automate)
            return;

        ServerResource resource = event.getHandle().getOrNull();
        if (resource == null)
            return;
        acquireImmediate(resource);
    }

    /* ------ Worker ------ */

    class WorkerTarget implements ThrowableRunnable {

        @Override
        public void run() throws Throwable {
            // while active
            while (worker.shouldRun()) {
                // wait for content
                if (queue.isEmpty()) {
                    synchronized (queue) {
                        waiting.set(true);
                        queue.wait();
                    }

                    waiting.set(false);
                }

                // handle all resources in queue
                List<ServerResource> resources = new ArrayList<>(queue.size());
                synchronized (queue) {
                    while (!queue.isEmpty())
                        resources.add(queue.poll());
                }

                for (ServerResource resource : resources) {
                    disposeImmediate(resource);
                }
            }
        }

    }

}
