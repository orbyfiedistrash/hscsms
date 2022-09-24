package net.orbyfied.hscsms.core.resource.event;

import net.orbyfied.hscsms.core.resource.ServerResourceHandle;
import net.orbyfied.hscsms.core.resource.ServerResourceManager;

@SuppressWarnings("rawtypes")
public class ResourceHandleReleaseEvent extends ResourceHandleEvent {

    public ResourceHandleReleaseEvent(ServerResourceManager manager, ServerResourceHandle handle) {
        super(manager, handle);
    }

}
