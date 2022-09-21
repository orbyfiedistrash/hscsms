package net.orbyfied.hscsms.client.applib.impl;


import net.orbyfied.hscsms.client.applib.AppContext;
import net.orbyfied.hscsms.client.applib.AppContextManager;
import net.orbyfied.hscsms.util.Values;

public class ExitAC extends AppContext {
    public ExitAC(AppContextManager manager) {
        super(manager, "__exit");
    }

    @Override
    public void enter(Values values) {
        manager.getApp().quit();
    }

    @Override
    public void exit() {

    }
}
