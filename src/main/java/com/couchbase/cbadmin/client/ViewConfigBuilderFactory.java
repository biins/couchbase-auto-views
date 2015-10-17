package com.couchbase.cbadmin.client;

/**
 * @author Martin Janys
 */
public class ViewConfigBuilderFactory {

    private final String designName;
    private final String bucketName;
    private final String bucketPassword;

    public ViewConfigBuilderFactory(String designName) {
        this(designName, null, null);
    }

    public ViewConfigBuilderFactory(String designName, String bucketName, String bucketPassword) {
        this.bucketPassword = bucketPassword;
        this.designName = designName;
        this.bucketName = bucketName;
    }

    public ViewConfigBuilder create(String bucketName) {
        return create(bucketName, null, null);
    }

    public ViewConfigBuilder create(String designName, String bucketName, String bucketPassword) {
        ViewConfigBuilder viewConfigBuilder = new ViewConfigBuilder(designName, bucketName);
        if (bucketPassword != null)
            viewConfigBuilder.password(bucketPassword);
        return viewConfigBuilder;
    }
}
