package net.orbyfied.hscsms.client.applib.display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BiConsumer;

/**
 * Display/window for UI stuff using Java Swing.
 */
public class UIDisplay {

    UIDisplay parent;
    Window frame;
    String title;
    Dimension size;
    boolean important;

    final UIDisplayManager manager;
    final String name;

    public UIDisplay(UIDisplayManager manager, String name) {
        this.manager = manager;
        this.name    = name;
    }

    public UIDisplay setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public UIDisplay setParent(UIDisplay parent) {
        this.parent = parent;
        return this;
    }

    public UIDisplay setImportant(boolean b) {
        this.important = b;
        return this;
    }

    public boolean isImportant() {
        return important;
    }

    public UIDisplay setSize(int w, int h) {
        this.size = new Dimension(w, h);
        return this;
    }

    public UIDisplay setSize(Dimension size) {
        this.size = size;
        return this;
    }

    public Dimension getSize() {
        return size;
    }

    public UIDisplay window(BiConsumer<UIDisplay, Window> consumer) {
        consumer.accept(this, frame);
        return this;
    }

    public UIDisplay create() {
        if (parent == null)
            frame = new JFrame("HSCSMS Client :: " + title);
        else
            frame = new JDialog((JFrame)parent.frame, "HSCSMS Client :: " + title);
        frame.setIconImage(manager.iconImage);
        if (size != null)
            frame.setPreferredSize(size);
        if (isImportant())
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    manager.app
                            .appContextManager
                            .setCurrentContext("__exit");
                }
            });
        frame.pack();
        return this;
    }

    public UIDisplay destroy() {
        manager.remove(this);
        return this;
    }

    public UIDisplay show(boolean b) {
        frame.setVisible(b);
        frame.repaint();
        return this;
    }

    //////////////////////////////////////////////////////////////

    public static class UFrame extends JFrame {
        public UFrame(String title) {
            super(title);
        }

        @Override
        public void paint(Graphics gg) {
            Graphics2D g = (Graphics2D) gg;
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

}
