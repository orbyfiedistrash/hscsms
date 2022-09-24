package net.orbyfied.hscsms.core.resource.event;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;

public class ResourceCreateEvent extends ResourceEvent {

    public ResourceCreateEvent(ServerResourceManager manager, ServerResource resource) {
        super(manager, resource);
    }

}
