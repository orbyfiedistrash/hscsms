package net.orbyfied.hscsms.client.app;

import net.orbyfied.hscsms.client.applib.*;
import net.orbyfied.hscsms.client.applib.display.UIDisplay;

import java.awt.*;

public class ConnectScreenAC extends AppContext {

    public ConnectScreenAC(AppContextManager manager) {
        super(manager, "connect_screen");
    }

    UIDisplay display;

    @Override
    public void enter() {
        // create display
        display = manager.getApp().displayManager.create("connect_screen");
        // setup display
        display.setTitle("Connect")
                .setImportant(true)
                .setSize(800, 600)
                .create()
                .window((uiDisplay, window) -> {
                    window.setBackground(Color.DARK_GRAY);
                })
                .show(true);
    }

    @Override
    public void exit() {
        // destroy display
        display.show(false);
        display.destroy();
    }

}
