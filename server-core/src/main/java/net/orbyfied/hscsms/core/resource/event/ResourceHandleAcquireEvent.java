package net.orbyfied.hscsms.core.resource.event;

import net.orbyfied.hscsms.core.resource.ServerResourceHandle;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;

@SuppressWarnings("rawtypes")
public class ResourceHandleAcquireEvent extends ResourceHandleEvent {

    public ResourceHandleAcquireEvent(ServerResourceManager manager, ServerResourceHandle handle) {
        super(manager, handle);
    }

}
