package net.orbyfied.hscsms.core.resource;

import net.orbyfied.j8.event.EventListener;
import net.orbyfied.j8.event.RegisteredListener;

public abstract class AbstractResourceService
        implements ResourceService, /* automatically make this an event listener */ EventListener {

    // the service holder
    // aka the server resource manager
    protected final ServerResourceManager manager;

    // if this service should register
    // itself as an event listener
    protected boolean registerEvents = true;
    // the registered listener instance
    protected RegisteredListener listener;

    public AbstractResourceService(ServerResourceManager manager) {
        this.manager = manager;
    }

    @Override
    public void added() {
        // register self to event bus
        if (registerEvents)
            listener = manager.eventBus.register(this);
    }

    @Override
    public void removed() {
        // unregister self from event bus
        if (listener != null)
            manager.eventBus.unregister(listener);
    }

    @Override
    public ServerResourceManager manager() {
        return this.manager;
    }

}
