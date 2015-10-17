package org.biins.cauchbase;

import com.couchbase.cbadmin.assets.Bucket.BucketType;

/**
 * @author Martin Janys
 */
public @interface Bucket {

    String name();

    String password() default "";

    BucketType bucketType() default BucketType.MEMCACHED;

    int ramQuotaMB() default 100;

    int replicaCount() default 0;

    boolean shouldIndexReplicas() default false;

}
