package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.ViewConfig;
import com.couchbase.cbadmin.client.ViewConfigBuilder;
import org.biins.cauchbase.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Martin Janys
 */
public class ViewBuilder {

    private static final String CLASS_PATH_PREFIX = "classpath:";


    public ViewConfig build(View view) {
        ViewConfigBuilder configBuilder = new ViewConfigBuilder(
                view.design(),
                !view.bucket().isEmpty() ? view.bucket() : null);

        return configBuilder.build();
    }
    public ViewConfig build(View view, String designName, String bucketName, String password) {
        ViewConfigBuilder configBuilder = new ViewConfigBuilder(designName, bucketName);
        configBuilder.password(password);
        return configBuilder
                .view(view.name(), resolveFunction(view.map()), !view.reduce().isEmpty() ? resolveFunction(view.reduce()) : null)
                .build();
    }

    private String resolveFunction(String func) {
        if (func.startsWith(CLASS_PATH_PREFIX)) {
            return readClassPathResource(func);
        }
        else {
            return func;
        }
    }

    private String readClassPathResource(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(parseClasspathPath(path))));
        String lineSeparator = String.format("%n");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(lineSeparator);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private String parseClasspathPath(String path) {
        String prefix = CLASS_PATH_PREFIX + "/";
        if (!path.startsWith(prefix)) {
            throw new IllegalArgumentException("Set absolute classpath path starts with '/'. For example classpath:/path/to/script.js");
        }
        else {
            return path.substring(prefix.length());
        }
    }

}
