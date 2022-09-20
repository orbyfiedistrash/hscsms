package net.orbyfied.hscsms.client.applib.display;

import java.awt.*;

public class DisplayUtil {

    public static final Dimension NORMAL_SCREEN_ASPECT = new Dimension(16, 9);
    public static final Dimension WIDE_SCREEN_ASPECT   = new Dimension(21, 9);

    public static Dimension getScaledDimensions(Dimension aspect, float mul) {
        return new Dimension((int) (aspect.width * mul), (int) (aspect.height * mul));
    }

    public static Dimension getScaledDimensions(Dimension aspect, int width) {
        float mul = width / (aspect.width * 1f);
        return getScaledDimensions(aspect, mul);
    }

}
