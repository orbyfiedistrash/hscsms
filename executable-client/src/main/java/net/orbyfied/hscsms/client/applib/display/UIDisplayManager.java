package net.orbyfied.hscsms.client.applib.display;

import net.orbyfied.hscsms.client.app.ClientApp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIDisplayManager {

    final ClientApp app;

    final List<UIDisplay> displays = new ArrayList<>(3);
    final Map<String, UIDisplay> displaysByName = new HashMap<>(3);

    // icon image
    BufferedImage iconImage;

    public UIDisplayManager(ClientApp app) {
        this.app = app;
    }

    void loadIconImage() {
        try {
            iconImage = ImageIO.read(app.getAssetFolder().resolve("icon-750x750.png").toFile());
        } catch (Exception e) {
            System.err.println("UIDisplayManager: Failed to load icon image;");
            e.printStackTrace();
        }
    }

    void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UIDisplayManager load() {
        loadIconImage();
        initLookAndFeel();
        return this;
    }

    public UIDisplay create(String name) {
        UIDisplay display = new UIDisplay(this, name);
        displays.add(display);
        displaysByName.put(name, display);
        return display;
    }

    public List<UIDisplay> getDisplays() {
        return displays;
    }

    public Map<String, UIDisplay> getDisplaysByName() {
        return displaysByName;
    }

    public UIDisplay getDisplayByName(String name) {
        return displaysByName.get(name);
    }

    public UIDisplayManager remove(UIDisplay display) {
        displays.remove(display);
        displaysByName.remove(display.name);
        return this;
    }

}
