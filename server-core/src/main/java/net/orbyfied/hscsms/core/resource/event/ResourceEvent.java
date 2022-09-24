package net.orbyfied.hscsms.core.resource.event;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;
import net.orbyfied.j8.event.BusEvent;

public abstract class ResourceEvent extends BusEvent {

    // the resource manager
    final ServerResourceManager manager;
    // the resource this event was called on
    final ServerResource resource;

    public ResourceEvent(ServerResourceManager manager,
                         ServerResource resource) {
        this.manager  = manager;
        this.resource = resource;
    }

    public ServerResource getResource() {
        return resource;
    }

    public ServerResourceManager getManager() {
        return manager;
    }

}
