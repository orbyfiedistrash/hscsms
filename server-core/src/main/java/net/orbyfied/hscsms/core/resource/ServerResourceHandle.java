package net.orbyfied.hscsms.core.resource;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ServerResourceHandle<R extends ServerResource> {

    // the resource manager
    final ServerResourceManager manager;
    // the universal uuid
    final UUID uuid;

    // the loaded resource if loaded
    // or null if unloaded
    WeakReference<R> resource;
    // if the resource has been acquired
    volatile boolean acquired = false;

    public ServerResourceHandle(ServerResourceManager manager, UUID uuid) {
        this.manager = manager;
        this.uuid    = uuid;
    }

    public ServerResourceHandle(ServerResourceManager manager, R resource) {
        this.manager  = manager;
        this.uuid     = resource.universalID();
        this.resource = new WeakReference<>(resource);
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
     * Check if the universal ID is
     * a null resource ID.
     * @see ServerResourceManager#NULL_ID
     * @return True if it is a NULL_ID.
     */
    public boolean isNullID() {
        return ServerResourceManager.NULL_ID.equals(uuid);
    }

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
            resource = new WeakReference<>(manager.loadResourceUnwrapped(uuid));
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
                R res = manager.loadResourceUnwrapped(uuid);
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

    /**
     * Provides the resource to the consumer if
     * loaded, or doesn't call the consumer if not.
     * @param consumer The consumer.
     * @return This.
     */
    public ServerResourceHandle<R> useOrNot(Consumer<R> consumer) {
        R resource = getOrNull();
        if (resource != null)
            consumer.accept(resource);
        return this;
    }

    /**
     * Provides the resource to the consumer if
     * loaded, or calls it with null if not.
     * @param consumer The consumer.
     * @return This.
     */
    public ServerResourceHandle<R> useOrNull(Consumer<R> consumer) {
        R resource = getOrNull();
        consumer.accept(resource);
        return this;
    }

    /**
     * Provides the resource to the consumer if
     * loaded, loads it and calls it if not loaded.
     * @param consumer The consumer.
     * @return This.
     */
    public ServerResourceHandle<R> useOrLoad(Consumer<R> consumer) {
        R resource = getOrLoad();
        consumer.accept(resource);
        return this;
    }

    /**
     * @see ServerResourceHandle#acquire()
     * @see ServerResourceHandle#getOrNull()
     * @return The loaded resource or null.
     */
    public R acquireAndGet() {
        return acquire().getOrNull();
    }

    /**
     * @see ServerResourceHandle#acquire()
     * @see ServerResourceHandle#getOrNull()
     * @return The loaded resource or null.
     */
    public ServerResourceHandle<R> acquireAndUse(Consumer<R> consumer) {
        R resource = acquire().getOrNull();
        if (consumer != null)
            consumer.accept(resource);
        return this;
    }

    /**
     * Marks this resource as used so it is,
     * for example, not collected by the garbage
     * collector. This allows you to use it without
     * it being unloaded. It is set in this state by
     * default by the resource manager.
     * @return This.
     */
    public ServerResourceHandle<R> acquire() {
        // check if acquired
        if (!acquired) {
            // call into manager
            manager.doHandleAcquire(this);

            // set acquired
            acquired = true;
        }

        // return
        return this;
    }

    /**
     * Marks this resource as unused, so it
     * can be, for example, unloaded by the
     * garbage collector. When you dispose
     * the handle you're essentially saying
     * you won't need it anymore, though
     * nothing prevents you from using it
     * after disposal until it is unloaded.
     * @return This.
     */
    public ServerResourceHandle<R> release() {
        // check if acquired
        if (acquired) {
            // call into manager
            manager.doHandleRelease(this);

            // set released
            acquired = false;
        }

        // return
        return this;
    }

}
