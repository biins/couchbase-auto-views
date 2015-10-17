package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.ViewConfig;
import com.couchbase.cbadmin.client.ViewConfigBuilderFactory;
import org.biins.cauchbase.View;
import org.biins.cauchbase.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Janys
 */
public class ViewsBuilder {

    private final ViewConfigBuilderFactory configBuilderFactory;
    private final ViewBuilder viewBuilder;

    public ViewsBuilder(ViewConfigBuilderFactory configBuilderFactory) {
        this.configBuilderFactory = configBuilderFactory;
        this.viewBuilder = new ViewBuilder(configBuilderFactory);
    }

    public List<ViewConfig> build(Views views) {
        List<ViewConfig> result = new ArrayList<ViewConfig>(views.value().length);
        for (View view : views.value()) {
            result.add(viewBuilder.build(view));
        }
        return result;
    }

}
