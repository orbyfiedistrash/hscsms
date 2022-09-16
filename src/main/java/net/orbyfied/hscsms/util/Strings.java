package net.orbyfied.hscsms.util;

import java.util.Objects;

public class Strings {

    public static String toStringPretty(Object val) {
        if (val instanceof String)
            return "\"" + val + "\"";
        return Objects.toString(val);
    }

}
