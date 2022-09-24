package net.orbyfied.hscsms.core.resource.event;

import net.orbyfied.hscsms.core.resource.ServerResource;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;

public class ResourceUnloadEvent extends ResourceEvent {

    public ResourceUnloadEvent(ServerResourceManager manager, ServerResource resource) {
        super(manager, resource);
    }

}
