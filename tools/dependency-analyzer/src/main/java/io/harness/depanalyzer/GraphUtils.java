/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.depanalyzer;

import com.google.common.graph.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GraphUtils {

    public static void printGraph(Graph<String> graph) {
        System.out.println("Graph:");
        for (String node : graph.nodes()) {
            System.out.print(node + " -> ");
            for (String adjacentNode : graph.successors(node)) {
                System.out.print(adjacentNode + " ");
            }
            System.out.println();
        }
    }

    public static void printDisconnectedComponents(MutableGraph<String> graph) {
        List<Set<String>> disconnectedComponents = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String node : graph.nodes()) {
            if (!visited.contains(node)) {
                Set<String> component = new HashSet<>();
                depthFirstSearch(graph, node, visited, component);
                disconnectedComponents.add(component);
            }
        }

        System.out.println("Disconnected Components:");
        for (Set<String> component : disconnectedComponents) {
            System.out.println("- " + component);
        }
    }

    private static void depthFirstSearch(MutableGraph<String> graph, String node, Set<String> visited, Set<String> component) {
        visited.add(node);
        component.add(node);

        for (String neighbor : graph.successors(node)) {
            if (!visited.contains(neighbor)) {
                depthFirstSearch(graph, neighbor, visited, component);
            }
        }
    }


    public static void printIndependentNodes(MutableGraph<String> graph) {
        Set<String> independentNodes = graph.nodes();
        for (EndpointPair<String> edge : graph.edges()) {
            String source = edge.source();
            independentNodes.remove(source); // Remove nodes with outgoing edges
        }
        System.out.println("Independent Nodes (no outgoing dependencies):");
        System.out.println(independentNodes);
    }

    public static void printNodesByLevel(MutableGraph<String> graph) {
        // Implementation using Guava's graph traversal methods or iterators
        // (implementation not shown for brevity)
    }

    public static MutableGraph<String> createPackageGraph(MutableGraph<String> fileGraph, Map<String, Set<String>> packageFiles) {
        MutableGraph<String> packageGraph = GraphBuilder.directed().build();
        for (String packageName : packageFiles.keySet()) {
            packageGraph.addNode(packageName);
        }

        for (EndpointPair<String> edge : fileGraph.edges()) {
            String sourceFile = edge.source();
            String targetFile = edge.target();
            Path sourceParentDirPath = Paths.get(sourceFile).getParent();
            Path targetParentDirPath = Paths.get(targetFile).getParent();

            // Use the path strings directly as package
            String sourcePackage = sourceParentDirPath.toString();
            String targetPackage = targetParentDirPath.toString();

            if (!sourcePackage.equals(targetPackage)) {
                packageGraph.putEdge(sourcePackage, targetPackage);
            }
        }

        return packageGraph;
    }

    public static void printIndependentPackages(MutableGraph<String> packageGraph, Map<String, Set<String>> packageFiles, String targetDirectory) {
        Set<String> independentPackages = new HashSet<>(packageGraph.nodes()); // Start with all packages

        for (String packageNode : packageGraph.nodes()) {
            if (packageGraph.successors(packageNode).stream()
                    // Check for incoming edges from packages within the target directory
                    .anyMatch(source -> packageFiles.get(source).stream()
                            .anyMatch(file -> file.startsWith(targetDirectory)))) {
                independentPackages.remove(packageNode); // Remove dependent packages
            }
        }

        System.out.println("Independent Packages (no dependencies within " + targetDirectory + "):");
        System.out.println(independentPackages);
    }

    public static void findAllCycles(MutableGraph<String> graph) {
        List<Set<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String node : graph.nodes()) {
            if (!visited.contains(node)) {
                findCycles(graph, node, visited, new HashSet<>(), new ArrayList<>(), cycles);
            }
        }

        if (cycles.isEmpty()) {
            System.out.println("No cycles found in the graph.");
        } else {
            System.out.println("Cycles found:");
            for (int i = 0; i < cycles.size(); i++) {
                System.out.println("Cycle " + (i + 1) + ": " + cycles.get(i));
            }
        }
    }

    private static void findCycles(MutableGraph<String> graph, String node, Set<String> visited, Set<String> currentPath, List<String> currentCycle, List<Set<String>> cycles) {
        visited.add(node);
        currentPath.add(node);
        currentCycle.add(node); // Track nodes in the current cycle

        for (String neighbor : graph.successors(node)) {
            if (visited.contains(neighbor)) {
                if (currentCycle.contains(neighbor)) { // Cycle found
                    cycles.add(new HashSet<>(currentCycle.subList(currentCycle.indexOf(neighbor), currentCycle.size())));
                } else {
                    findCycles(graph, neighbor, visited, currentPath, currentCycle, cycles); // Explore for more cycles
                }
            } else if (!currentPath.contains(neighbor)) {
                findCycles(graph, neighbor, visited, currentPath, currentCycle, cycles);
            }
        }

        currentPath.remove(node);
        currentCycle.remove(currentCycle.size() - 1); // Backtrack in cycle tracking
    }

    public static void exportGraphToDot(MutableGraph<String> graph, String filename) throws IOException {
        StringBuilder dotString = new StringBuilder("digraph G {\n");

        // Add nodes
        for (String node : graph.nodes()) {
            dotString.append("  ").append(node).append(";\n");
        }

        // Add edges
        for (EndpointPair<String> edge : graph.edges()) {
            dotString.append("  ").append(edge.nodeU()).append(" -> ").append(edge.nodeV()).append(";\n");
        }

        dotString.append("}\n");

        // Write DOT string to file
        Files.writeString(Paths.get(filename), dotString.toString());
    }

}
