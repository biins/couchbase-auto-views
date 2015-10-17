package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.BucketConfig;
import org.biins.cauchbase.Bucket;

/**
 * @author Martin Janys
 */
public class BucketBuilder {

    public BucketConfig build(Bucket bucket) {
        BucketConfig bucketConfig = new BucketConfig(bucket.name());

        if (!bucket.password().isEmpty()) {
            bucketConfig.setSaslPassword(bucket.password());
        }

        bucketConfig.bucketType = bucket.bucketType();
        bucketConfig.ramQuotaMB = bucket.ramQuotaMB();
        bucketConfig.replicaCount = bucket.replicaCount();
        bucketConfig.shouldIndexReplicas = bucket.shouldIndexReplicas();

        return bucketConfig;
    }
}
