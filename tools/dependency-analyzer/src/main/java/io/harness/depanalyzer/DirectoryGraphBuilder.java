/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.depanalyzer;

import com.google.common.graph.MutableGraph;
import com.google.common.graph.GraphBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class DirectoryGraphBuilder {
    private MutableGraph<Path> graph;

    public DirectoryGraphBuilder() {
        this.graph = GraphBuilder.directed().allowsSelfLoops(true).build();
    }

//    private List<Path> getDependencies(Path source) {
//        // Implement the logic to determine dependencies for the source directory
//        // This is a stub and needs to be filled in based on your specific logic
//        return List.of();
//    }

    public MutableGraph<Path> getGraph() {
        return graph;
    }
}
