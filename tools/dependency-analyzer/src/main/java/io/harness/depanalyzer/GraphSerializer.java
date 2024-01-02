/*
 * Copyright 2024 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.depanalyzer;

import com.google.common.graph.MutableGraph;
import com.google.common.graph.GraphBuilder;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.*;

public class GraphSerializer {

    public static void saveGraph(MutableGraph<Path> graph, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Path node : graph.nodes()) {
                // Write each node
                writer.write("Node: " + node);
                writer.newLine();

                // Write the edges for the node
                for (Path edge : graph.successors(node)) {
                    writer.write("Edge: " + node + " -> " + edge);
                    writer.newLine();
                }
            }
        }
    }

    public static MutableGraph<Path> loadGraph(String filename) throws IOException {
        MutableGraph<Path> graph = GraphBuilder.directed().allowsSelfLoops(true).build();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Node: ")) {
                    Path node = Paths.get(line.substring(6));
                    graph.addNode(node);
                } else if (line.startsWith("Edge: ")) {
                    String[] parts = line.substring(6).split(" -> ");
                    graph.putEdge(Paths.get(parts[0]), Paths.get(parts[1]));
                }
            }
        }

        return graph;
    }
}
