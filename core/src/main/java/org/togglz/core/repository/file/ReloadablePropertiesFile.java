package org.togglz.core.repository.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.togglz.core.logging.Log;
import org.togglz.core.logging.LogFactory;
import org.togglz.core.util.IOUtils;

class ReloadablePropertiesFile {

    private final Log log = LogFactory.getLog(ReloadablePropertiesFile.class);

    private final File file;

    private Properties values = new Properties();

    private long lastRead = 0;

    public ReloadablePropertiesFile(File file) {
        this.file = file;
    }

    public synchronized void reloadIfUpdated() {

        if (file.lastModified() > lastRead) {

            FileInputStream stream = null;

            try {

                // read new values
                stream = new FileInputStream(file);
                Properties newValues = new Properties();
                newValues.load(stream);

                // update state
                values = newValues;
                lastRead = System.currentTimeMillis();

                log.info("Reloaded file: " + file.getCanonicalPath());

            } catch (FileNotFoundException e) {
                log.debug("File not found: " + file);
            } catch (IOException e) {
                log.error("Failed to read file", e);
            } finally {
                IOUtils.close(stream);
            }

        }

    }

    public String getValue(String key, String defaultValue) {
        return values.getProperty(key, defaultValue);
    }

    public Set<String> getKeysStartingWith(String prefix) {

        Set<String> result = new HashSet<String>();

        Enumeration<?> keys = values.propertyNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }

        return result;

    }

    public Editor getEditor() {
        return new Editor(values);
    }

    private void write(Properties newValues) {

        try {

            FileOutputStream fos = new FileOutputStream(file);
            newValues.store(fos, null);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to write new values", e);
        }
        lastRead = 0;

    }

    public class Editor {

        private Properties newValues;

        private Editor(Properties props) {
            newValues = new Properties();
            newValues.putAll(props);
        }

        public void setValue(String key, String value) {
            if (value != null) {
                newValues.setProperty(key, value);
            }
            else {
                newValues.remove(key);
            }
        }

        public void removeKeysStartingWith(String prefix) {
            Iterator<Entry<Object, Object>> iterator = newValues.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Object, Object> entry = iterator.next();
                if (entry.getKey().toString().startsWith(prefix)) {
                    iterator.remove();
                }
            }
        }

        public void commit() {
            write(newValues);
        }

    }

}
