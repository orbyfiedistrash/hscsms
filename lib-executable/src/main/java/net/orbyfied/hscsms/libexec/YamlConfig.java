package net.orbyfied.hscsms.libexec;

import net.orbyfied.hscsms.util.Values;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class YamlConfig {

    // yaml instance
    private static final Yaml yaml = new Yaml();

    //////////////////////////////////////////

    /**
     * @param file Disk file path.
     * @param src The resource source.
     * @param rp The resource path.
     * @return The loaded values.
     */
    public static Values copyDefaultsAndLoad(Path file, Class<?> src, String rp) {
        // fix resource path
        if (!rp.startsWith("/"))
            rp = "/" + rp;

        try {
            // check if file is present
            if (Files.exists(file)) {
                // load current values
                Map<String, Object> values = yaml.load(Files.newInputStream(file));

                // load default values
                Map<String, Object> defaultValues = yaml.load(src.getResourceAsStream(rp));

                // check (optional) version and
                // copy absent defaults
                Integer cv = (Integer) values.get("=version");
                Integer dv = (Integer) defaultValues.get("=version");
                if (cv != null && dv != null &&
                        cv < dv) {
                    // copy absent defaults
                    copyAbsentDefaults(values, defaultValues);

                    // save to file asynchronously
                    CompletableFuture.runAsync(() -> {
                        try {
                            OutputStream stream = Files.newOutputStream(file);
                            yaml.dump(values, new OutputStreamWriter(stream));
                            stream.flush();
                            stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                // return values
                return new Values(values);
            } else /* force copy all defaults */ {
                // create absent file
                Files.createDirectories(file.getParent());
                Files.createFile(file);

                // open streams to resource and file
                OutputStream fos = Files.newOutputStream(file);
                InputStream  ris = src.getResourceAsStream(rp);
                ris.transferTo(fos);
                fos.flush();
                ris.close();
                fos.close();

                // load new file
                InputStream fis = Files.newInputStream(file);
                Map<String, Object> vals = yaml.load(fis);
                fis.close();
                return new Values(vals);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyAbsentDefaults(Map current, Map<?, ?> defaults) {
        for (Map.Entry<?, ?> entry : defaults.entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            if (!current.containsKey(k)) {
                current.put(k, v);
            } else {
                Object cv = current.get(k);
                if (cv instanceof Map mc && v instanceof Map md) {
                    copyAbsentDefaults(mc, md);
                }
            }
        }
    }

}
