# couchbase-auto-view

## Maven
```
<dependency>
    <groupId>org.biins</groupId>
    <artifactId>couchbase-auto-view</artifactId>
    <version>1.0</version>
</dependency>
```

## About

* this artefact provide support for auto-creation or update views in Couchbase

## Examples

### Bucket setup
```
@Bucket(name = "myBucket", type = BucketType.COUCHBASE, design = "myDesign")
public class CouchbaseDao implements InitializingBean {
}
```

### View using javascript function
```
@View(name = "all", map = "function (doc, meta) { emit(meta.id, null) }")
public List<JsonDocument> findAll() {
}
```
### View using javascript file
```
@View(name = "all", map = "classpath:/script/animal/map_all.js")
public List<JsonDocument> findAll() {
}
```
```
@View(name = "all", map = "classpath:/script/animal/map_all.js")
public List<JsonDocument> findAll() {
}
```
```
@View(name = "count", map = "classpath:/script/animal/map_all.js", reduce = "_count")
public int count() {
}
```

### Initialization
* Java config
```
@Configuration
@PropertySource("classpath:/couchbase.properties")
public class CouchbaseAutoViewConfig extends Config {

    @Value("${couchbase.admin.node}") String node;
    @Value("${couchbase.admin.username}") String username;
    @Value("${couchbase.admin.password}") String password;

    @Value("${couchbase.development.views}") boolean developmentViews;

    @Bean
    public CouchbaseAdmin couchbaseAdmin() throws MalformedURLException {
        return new CouchbaseAdminImpl(new URL(node), username, password);
    }

    @Bean
    public AutoViews autoViews(CouchbaseAdmin couchbaseAdmin) {
        AutoViews autoViews = new AutoViews(couchbaseAdmin);
        autoViews.setDevelopmentViews(developmentViews);
        return autoViews;
    }

}
```
* Dao initialization
```
@Bucket(name = "myBucket", type = BucketType.COUCHBASE, design = "myDesign")
public class CouchbaseDao implements InitializingBean {
    @Autowire
    private AutoViews autoViews;

    public void afterPropertiesSet() throws Exception {
        autoViews.setup(this);
    }

    // @View ...
}
```
