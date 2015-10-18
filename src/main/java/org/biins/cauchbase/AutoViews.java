package org.biins.cauchbase;

import com.couchbase.cbadmin.client.BucketConfig;
import com.couchbase.cbadmin.client.CouchbaseAdmin;
import com.couchbase.cbadmin.client.RestApiException;
import com.couchbase.cbadmin.client.ViewConfig;
import org.biins.cauchbase.builder.BucketBuilder;
import org.biins.cauchbase.builder.ViewBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.biins.cauchbase.AnnotationUtils.*;

/**
 * @author Martin Janys
 */
public class AutoViews {

    private final CouchbaseAdmin client;

    private Map<String, String> bucketPasswords = Collections.emptyMap();
    private long pollTimeout = 5000;

    public AutoViews(CouchbaseAdmin client) {
        this.client = client;
    }

    public void setBucketPasswords(Map<String, String> bucketPasswords) {
        this.bucketPasswords = bucketPasswords;
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setup(Object object) {
        Class<?> cls = object.getClass();
        setup(cls);
    }

    public void setup(Class<?> cls) {
        Bucket rootBucket = setupClass(cls);
        setupMethods(cls.getMethods(), rootBucket);
    }

    private Bucket setupClass(Class<?> cls) {
        Map<Class<?>, List<Annotation>> annotations = annotationsByTypes(cls.getAnnotations());

        List<Bucket> buckets = get(annotations, Bucket.class);
        if (buckets == null || buckets.size() != 1) {
            throw new IllegalStateException("One bucket must be defined");
        }
        Bucket rootBucket = buckets.get(0);
        List<View> views = get(annotations, View.class);

        List<BucketConfig> bucketConfigs = createBucketConfigs(buckets);
        List<ViewConfig> viewConfigs = createViewConfigs(views, rootBucket);

        createBuckets(bucketConfigs);
        createViews(viewConfigs);

        return rootBucket;
    }

    private List<BucketConfig> createBucketConfigs(List<Bucket> bucket) {
        List<BucketConfig> bucketConfigs = new ArrayList<>();
        if (bucket != null) {
            for (Bucket b : bucket) {
                BucketBuilder bucketBuilder = new BucketBuilder();
                BucketConfig config = bucketBuilder.build(b, bucketPasswords.get(b.name()));
                bucketConfigs.add(config);
            }
        }

        return bucketConfigs;
    }

    private List<ViewConfig> createViewConfigs(List<View> view, Bucket rootBucket) {
        List<ViewConfig> viewConfigs = new ArrayList<>();
        if (view != null) {
            for (View v : view) {
                viewConfigs.add(createViewConfig(v, rootBucket));
            }
        }
        return viewConfigs;
    }

    private ViewConfig createViewConfig(View v, Bucket rootBucket) {
        String bucketName = resolveBucketName(v, rootBucket);
        String designName = resolveDesignName(v, rootBucket);
        ViewBuilder viewBuilder = new ViewBuilder();
        return viewBuilder.build(v, designName, bucketName, bucketPasswords.get(bucketName));
    }

    private String resolveDesignName(View v, Bucket bucket) {
        if (!v.design().isEmpty()) {
            return v.design();
        }
        else {
            return bucket.design();
        }
    }

    private String resolveBucketName(View v, Bucket bucket) {
        if (!v.bucket().isEmpty()) {
            return v.bucket();
        }
        else {
            return bucket.name();
        }
    }

    private void setupMethods(Method[] methods, Bucket rootBucket) {
        for (Method method : methods) {
            setupMethod(method, rootBucket);
        }
    }

    private void setupMethod(Method method, Bucket rootBucket) {
        List<ViewConfig> viewConfigs = new ArrayList<>();
        List<View> views = annotationsByType(View.class, method.getAnnotations());
        for (View v : views) {
            ViewConfig viewConfig = createViewConfig(v, rootBucket);
            viewConfigs.add(viewConfig);
        }

        createViews(viewConfigs);
    }

    public void createBuckets(List<BucketConfig> bucketConfigs) {
        for (BucketConfig bucketConfig : bucketConfigs) {
            try {
                if (!client.getBuckets().containsKey(bucketConfig.name)) {
                    client.createBucket(bucketConfig);
                }
            }
            catch (RestApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void createViews(List<ViewConfig> viewConfigs) {
        for (ViewConfig viewConfig : viewConfigs) {
            try {
                client.defineView(viewConfig, pollTimeout);
            }
            catch (RestApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
