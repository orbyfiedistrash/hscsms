package net.orbyfied.hscsms.core.resource;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerResourceHandle<R extends ServerResource> {

    // the resource manager
    final ServerResourceManager manager;
    // the universal uuid
    final UUID uuid;

    // the loaded resource if loaded
    // or null if unloaded
    WeakReference<R> resource;

    public ServerResourceHandle(ServerResourceManager manager, UUID uuid) {
        this.manager = manager;
        this.uuid    = uuid;
    }

    /* Getters */

    /**
     * @return The server resource manager.
     */
    public ServerResourceManager manager() {
        return manager;
    }

    /**
     * @return The universal ID.
     */
    public UUID universalID() {
        return uuid;
    }

    /* ----------------- */

    /**
     * Get the loaded resource, or null
     * if the resource is unloaded.
     * @return The resource or null.
     */
    public R getOrNull() {
        // check if unloaded
        if (resource == null)
            return null;
        // return loaded or null reference
        return resource.get();
    }

    /**
     * Get the loaded resource, or load
     * it is unloaded.
     * @return The resource.
     */
    public R getOrLoad() {
        // check loaded, if not load
        if (resource == null || resource.get() == null)
            resource = new WeakReference<>(manager.loadResource(uuid));
        // return already loaded
        return resource.get();
    }

    /**
     * Get the loaded resource, or load
     * it is unloaded.
     * @return The resource.
     */
    public CompletableFuture<R> getOrLoadAsync() {
        // check loaded
        if (resource == null || resource.get() == null) {
            return CompletableFuture.supplyAsync(() -> {
                // load the resource
                R res = manager.loadResource(uuid);
                // set resource reference
                synchronized (resource) {
                    resource = new WeakReference<>((R) res);
                }

                // return
                return res;
            });

        }

        // return already loaded
        CompletableFuture<R> future = new CompletableFuture<>();
        future.complete(resource.get());
        return future;
    }

}
