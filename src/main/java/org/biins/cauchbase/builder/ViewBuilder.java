package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.ViewConfig;
import com.couchbase.cbadmin.client.ViewConfigBuilder;
import com.couchbase.cbadmin.client.ViewConfigBuilderFactory;
import org.biins.cauchbase.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Martin Janys
 */
public class ViewBuilder {

    private static final String CLASS_PATH_PREFIX = "classpath:";

    private final ViewConfigBuilderFactory configBuilderFactory;

    public ViewBuilder(ViewConfigBuilderFactory configBuilderFactory) {
        this.configBuilderFactory = configBuilderFactory;
    }

    public ViewConfig build(View view) {
        ViewConfigBuilder configBuilder = configBuilderFactory.create(
                view.design(),
                !view.bucket().isEmpty() ? view.bucket() : null,
                !view.bucketPassword().isEmpty() ? view.bucketPassword() : null);

        return configBuilder
                .view(view.name(), resolveFunction(view.map()), resolveFunction(!view.reduce().isEmpty() ? view.reduce() : null))
                .build();
    }

    private String resolveFunction(String func) {
        if (func.startsWith(CLASS_PATH_PREFIX)) {
            return readClassPathResource(func.substring(CLASS_PATH_PREFIX.length()));
        }
        else {
            return func;
        }
    }

    private String readClassPathResource(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(path)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

}
