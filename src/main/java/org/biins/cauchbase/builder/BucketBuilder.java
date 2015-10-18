package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.BucketConfig;
import org.biins.cauchbase.Bucket;

/**
 * @author Martin Janys
 */
public class BucketBuilder {

    public BucketConfig build(Bucket bucket) {
        return build(bucket, null);
    }

    public BucketConfig build(Bucket bucket, String password) {
        BucketConfig bucketConfig = new BucketConfig(bucket.name());

        if (password != null) {
            bucketConfig.setSaslPassword(password);
        }

        bucketConfig.bucketType = bucket.type();
        bucketConfig.ramQuotaMB = bucket.ramQuotaMB();
        bucketConfig.replicaCount = bucket.replicaCount();
        bucketConfig.shouldIndexReplicas = bucket.shouldIndexReplicas();

        return bucketConfig;
    }
}
