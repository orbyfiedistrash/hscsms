package net.orbyfied.hscsms.client.applib;

public abstract class AppContext {

    protected final String name;
    protected final AppContextManager manager;

    public AppContext(AppContextManager manager, String name) {
        this.name = name;
        this.manager = manager;
    }

    public abstract void enter();

    public abstract void exit();

}
