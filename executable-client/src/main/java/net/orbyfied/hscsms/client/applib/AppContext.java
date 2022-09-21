package net.orbyfied.hscsms.client.applib;

import net.orbyfied.hscsms.util.Values;

public abstract class AppContext {

    protected final String name;
    protected final AppContextManager manager;

    public AppContext(AppContextManager manager, String name) {
        this.name = name;
        this.manager = manager;
    }

    public abstract void enter(Values values);

    public abstract void exit();

}
