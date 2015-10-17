package org.biins.cauchbase.builder;

import com.couchbase.cbadmin.client.BucketConfig;
import org.biins.cauchbase.Bucket;
import org.biins.cauchbase.Buckets;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Janys
 */
public class BucketsBuilder {

    private final BucketBuilder bucketBuilder;

    public BucketsBuilder() {
        this.bucketBuilder = new BucketBuilder();
    }

    public List<BucketConfig> build(Buckets buckets) {
        List<BucketConfig> result = new ArrayList<BucketConfig>(buckets.value().length);
        for (Bucket bucket : buckets.value()) {
            result.add(bucketBuilder.build(bucket));
        }
        return result;
    }
}
