package org.biins.cauchbase;

import com.couchbase.cbadmin.client.*;
import org.biins.cauchbase.builder.BucketHelper;
import org.biins.cauchbase.builder.ViewConfigHelper;

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
    private long pollTimeout = -1;
    private boolean developmentViews = false;

    public AutoViews(CouchbaseAdmin client) {
        this.client = client;
    }

    public void setBucketPasswords(Map<String, String> bucketPasswords) {
        this.bucketPasswords = bucketPasswords;
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setDevelopmentViews(boolean developmentViews) {
        this.developmentViews = developmentViews;
    }

    public void setup(Object object) {
        Class<?> cls = object.getClass();
        setup(cls);
    }

    public void setup(Class<?> cls) {
        List<Bucket> buckets = new ArrayList<>();
        List<View> views = new ArrayList<>();

        readClass(cls, buckets, views);
        if (buckets.size() != 1) {
            throw new IllegalStateException("One bucket must be defined");
        }
        Bucket rootBucket = buckets.get(0);
        readMethods(cls.getMethods(), rootBucket, views);

        createBucket(rootBucket);
        createViews(views, rootBucket);

    }

    private void readClass(Class<?> cls, List<Bucket> buckets, List<View> views) {
        Map<Class<?>, List<Annotation>> annotations = annotationsByTypes(cls.getAnnotations());
        buckets.addAll(get(annotations, Bucket.class));
        views.addAll(get(annotations, View.class));
    }

    private BucketConfig createBucketConfig(Bucket bucket) {
        return BucketHelper.build(bucket, bucketPasswords.get(bucket.name()));
    }

    private String resolveDesignName(Bucket bucket) {
        return developmentViews ? "dev_" + bucket.design() : bucket.design();
    }

    private void readMethods(Method[] methods, Bucket rootBucket, List<View> views) {
        for (Method method : methods) {
            readMethod(method, rootBucket, views);
        }
    }

    private void readMethod(Method method, Bucket rootBucket, List<View> views) {
        views.addAll(annotationsByType(View.class, method.getAnnotations()));
    }

    public void createBucket(Bucket bucket) {
        BucketConfig bucketConfig = createBucketConfig(bucket);
        try {
            if (!client.getBuckets().containsKey(bucketConfig.name)) {
                client.createBucket(bucketConfig);
            }
        }
        catch (RestApiException e) {
            throw new RuntimeException(e);
        }

    }

    public void createViews(List<View> views, Bucket rootBucket) {
        ViewConfigBuilder builder = null;
        for (View view : views) {
            if (builder == null) {
                builder = ViewConfigHelper.create(resolveDesignName(rootBucket), rootBucket.name(), bucketPasswords.get(rootBucket.name()));
            }

            ViewConfigHelper.addView(builder, view);
        }
        try {
            if (builder != null) {
                client.defineView(builder.build(), pollTimeout);
            }
        }
        catch (RestApiException e) {
            throw new RuntimeException(e);
        }
    }
}
