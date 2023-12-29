/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

import com.google.common.cache.*;

public class CachingUtils {

    private static final LoadingCache<String, Graph<String, DefaultEdge>> graphCache = CacheBuilder.newBuilder()
            .maximumSize(100) // Adjust maximum cache size as needed
            .expireAfterWrite(10, TimeUnit.MINUTES) // Expire entries after 10 minutes
            .build(new CacheLoader<String, Graph<String, DefaultEdge>>() {
                @Override
                public Graph<String, DefaultEdge> load(String key) throws Exception {
                    // Load graph from file using existing logic
                    return loadGraphFromSource(key);
                }
            });

    public static void cacheGraph(String key, Graph<String, DefaultEdge> graph) {
        // Guava cache now handles loading and persistence internally
        graphCache.put(key, graph);
    }

    public static Graph<String, DefaultEdge> getCachedGraph(String key) throws Exception {
        // Guava cache handles loading and persistence internally
        return graphCache.get(key);
    }

    private static Graph<String, DefaultEdge> loadGraphFromSource(String key) throws IOException, ClassNotFoundException {
        // Load graph from file using the provided logic
        try (FileInputStream fileIn = new FileInputStream("graph_cache.ser");
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            Map<String, Graph<String, DefaultEdge>> cache = (Map<String, Graph<String, DefaultEdge>>) in.readObject();
            return cache.get(key);
        }
    }
}
