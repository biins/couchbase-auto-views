package org.biins.cauchbase;

import com.couchbase.cbadmin.assets.Bucket.BucketType;

import java.lang.annotation.*;

/**
 * @author Martin Janys
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bucket {

    String name();

    String design();

    BucketType type() default BucketType.MEMCACHED;

    int ramQuotaMB() default 100;

    int replicaCount() default 0;

    boolean shouldIndexReplicas() default false;

}
