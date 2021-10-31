package net.twentyonesolutions.geoip;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

public class CachedMaxmindReader extends MaxmindReader {

    private LoadingCache<String, String> cache;

    public CachedMaxmindReader(String path, String spec) {
        super(path);
        this.createCache(spec);
    }

    public CachedMaxmindReader(String spec) {
        super();
        this.createCache(spec);
    }


    @Override
    public String getPathInfoJson(String addr) {
        try {
            return cache.get(addr);
        } catch (ExecutionException e) {
            return MaxmindReader.RECORD_NOT_FOUND_JSON;
        }
    }


    private void createCache(String spec) {

        cache = CacheBuilder
                    .from(spec)
                    // .removalListener(MY_LISTENER)
                    .build(
                        new CacheLoader<String, String>() {
                            public String load(String key) throws Exception {
                                if (key == null || key.isEmpty())
                                    return MaxmindReader.RECORD_NOT_FOUND_JSON;

                            //  System.out.println("cache miss: " + key);
                                return CachedMaxmindReader.super.getPathInfoJson(key);
                            }
                        }
                    );
    }
}
