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

import static org.biins.cauchbase.AnnotationUtils.annotationsByType;
import static org.biins.cauchbase.AnnotationUtils.annotationsByTypes;
import static org.biins.cauchbase.AnnotationUtils.get;

/**
 * @author Martin Janys
 */
public class AutoViews {

    private final CouchbaseAdmin client;

    private String designName;
    private Map<String, String> bucketPasswords;
    private String admin;
    private String adminPassword;
    private long pollTimeout = 5000;

    public AutoViews(CouchbaseAdmin client) {
        this.client = client;
    }

    public void setDesignName(String designName) {
        this.designName = designName;
    }

    public void setBucketPasswords(Map<String, String> bucketPasswords) {
        this.bucketPasswords = bucketPasswords;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setup(Object object) {
        Class<?> cls = object.getClass();
        setup(cls);
    }

    public void setup(Class<?> cls) {
        String rootBucket = setupClass(cls, bucketPasswords, admin, adminPassword);
        setupMethods(cls.getMethods(), rootBucket);
    }

    private String setupClass(Class<?> cls, Map<String, String> bucketPasswords, String admin, String adminPassword) {
        Map<Class<?>, List<Annotation>> annotations = annotationsByTypes(cls.getAnnotations());

        List<Bucket> bucket = get(annotations, Bucket.class);
        List<View> view = get(annotations, View.class);

        List<BucketConfig> bucketConfigs = createBucketConfigs(bucket);
        String rootBucket = bucketConfigs.size() == 1 ? bucket.get(0).name() : null;
        List<ViewConfig> viewConfigs = createViewConfigs(view, rootBucket);

        createBuckets(bucketConfigs);
        createViews(viewConfigs);

        return rootBucket;
    }

    private List<BucketConfig> createBucketConfigs(List<Bucket> bucket) {
        List<BucketConfig> bucketConfigs = new ArrayList<BucketConfig>();
        if (bucket != null) {
            for (Bucket b : bucket) {
                BucketBuilder bucketBuilder = new BucketBuilder();
                BucketConfig config = bucketBuilder.build(b, bucketPasswords.get(b.name()));
                bucketConfigs.add(config);
            }
        }

        return bucketConfigs;
    }

    private List<ViewConfig> createViewConfigs(List<View> view, String rootBucket) {
        List<ViewConfig> viewConfigs = new ArrayList<ViewConfig>();
        if (view != null) {
            for (View v : view) {
                viewConfigs.add(createViewConfig(v, rootBucket));
            }
        }
        return viewConfigs;
    }

    private ViewConfig createViewConfig(View v, String rootBucket) {
        String bucketName = resolveBucketName(v, rootBucket);
        ViewBuilder viewBuilder = new ViewBuilder();
        return viewBuilder.build(v, designName, bucketName, bucketPasswords.get(bucketName));
    }

    private String resolveBucketName(View v, String bucket) {
        if (!v.bucket().isEmpty()) {
            return v.bucket();
        }
        else {
            return bucket;
        }
    }

    private void setupMethods(Method[] methods, String rootBucket) {
        for (Method method : methods) {
            setupMethod(method, rootBucket);
        }
    }

    private void setupMethod(Method method, String rootBucket) {
        List<ViewConfig> viewConfigs = new ArrayList<ViewConfig>();
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
                client.createBucket(bucketConfig);
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
