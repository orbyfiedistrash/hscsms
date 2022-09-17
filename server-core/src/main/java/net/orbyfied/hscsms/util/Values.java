package net.orbyfied.hscsms.util;

import java.util.*;
import java.util.function.Function;

/**
 * General purpose key-value instance.
 * Unordered.
 */
public class Values {

    public static Values ofVarargs(Values values, Object... objs) {
        String key = null;
        int l = objs.length;
        for (int i = 0; i < l; i++) {
            Object obj = objs[i];
            if (i % 2 == 0) {
                key = (String) obj;
            } else {
                values.put(key, obj);
                key = null;
            }
        }

        return values;
    }

    public static Values ofVarargs(Object... objs) {
        return ofVarargs(new Values(), objs);
    }

    //////////////////////////////////////

    public Values() {
        map = new HashMap<>();
    }

    public Values(int size) {
        map = new HashMap<>(size);
    }

    public Values(Object... objs) {
        map = new HashMap<>(objs.length);
        ofVarargs(this, objs);
    }

    // the internal map
    HashMap<String, Object> map;

    public int getSize() {
        return map.size();
    }

    public HashMap<String, Object> getMap() {
        return map;
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    public List<Map.Entry<String, Object>> entries() {
        return new ArrayList<>(map.entrySet());
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public List<String> keys() {
        return new ArrayList<>(map.keySet());
    }

    public Collection<Object> valueCollection() {
        return map.values();
    }

    public List<Object> values() {
        return new ArrayList<>(map.values());
    }

    public Values set(String key, Object val) {
        map.put(key, val);
        return this;
    }

    public Values put(String key, Object val) {
        map.put(key, val);
        return this;
    }

    public Values putIfAbsent(String key, Object val) {
        map.putIfAbsent(key, val);
        return this;
    }

    public Values computeIfAbsent(String key, Function<String, Object> function) {
        map.computeIfAbsent(key, function);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        return (V) map.get(key);
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key, Class<V> vClass) {
        return (V) map.get(key);
    }

    /////////////////////////////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Values values = (Values) o;
        return Objects.equals(map, values.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{ ");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // handle trailing comma
            if (i != 0)
                b.append(", ");
            b.append("'" + entry.getKey() + "': ");
            b.append(Strings.toStringPretty(entry.getValue()));

            i++;
        }

        return b.toString();
    }
}
