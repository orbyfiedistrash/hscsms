package net.orbyfied.hscsms.client.applib.impl;


import net.orbyfied.hscsms.client.applib.AppContext;
import net.orbyfied.hscsms.client.applib.AppContextManager;

public class ExitAC extends AppContext {
    public ExitAC(AppContextManager manager) {
        super(manager, "__exit");
    }

    @Override
    public void enter() {
        manager.getApp().quit();
    }

    @Override
    public void exit() {

    }
}
