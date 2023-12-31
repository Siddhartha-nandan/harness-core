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

    public static void printGraph(Graph<Path> graph) {
        System.out.println("Printing graph showing dependencies");
        for (Path node : graph.nodes()) {
            System.out.print("Node " + node.toString() + " depends on: ");
            for (Path adjacentNode : graph.successors(node)) {
                System.out.print(adjacentNode.toString() + ", ");
            }
            System.out.println();
        }
    }

    public static void printDisconnectedComponents(MutableGraph<Path> graph) {
        List<Set<Path>> disconnectedComponents = new ArrayList<>();
        Set<Path> visited = new HashSet<>();

        for (Path node : graph.nodes()) {
            if (!visited.contains(node)) {
                Set<Path> component = new HashSet<>();
                depthFirstSearch(graph, node, visited, component);
                disconnectedComponents.add(component);
            }
        }

        System.out.println("Disconnected Components:");
        for (Set<Path> component : disconnectedComponents) {
            System.out.println("- " + component);
        }
    }

    private static void depthFirstSearch(MutableGraph<Path> graph, Path node, Set<Path> visited, Set<Path> component) {
        visited.add(node);
        component.add(node);

        for (Path neighbor : graph.successors(node)) {
            if (!visited.contains(neighbor)) {
                depthFirstSearch(graph, neighbor, visited, component);
            }
        }
    }

    public static void printNodesByLevel(MutableGraph<String> graph) {
        // Implementation using Guava's graph traversal methods or iterators
        // (implementation not shown for brevity)
    }

    public static void printIndependentPackages(MutableGraph<Path> graph, Path filterDirectory) {
        System.out.println("Independent Packages:");
//        for (Path node : graph.nodes()) {
//            if (graph.successors(node).isEmpty() && node.startsWith(filterDirectory)) {
//                System.out.println(node);
//            }
//        }
        // Step 1: Identify nodes in the specific directory
        Set<Path> nodesInDirectory = new HashSet<>();
        for (Path node : graph.nodes()) {
            if (node.startsWith(filterDirectory)) {
                nodesInDirectory.add(node);
            }
        }

        // Step 2: Check for dependencies and print nodes
        List<Path> eligibleNodes = new ArrayList<>();
        for (Path node : graph.nodes()) {
            boolean hasDependencyInDirectory = false;
            for (Path successor : graph.successors(node)) {
                if (nodesInDirectory.contains(successor)) {
                    hasDependencyInDirectory = true;
                    break;
                }
            }
            if (!hasDependencyInDirectory && node.startsWith(filterDirectory)) {
                eligibleNodes.add(node);
            }
        }

        Collections.sort(eligibleNodes);

        // Step 4: Print sorted nodes
        for (Path node : eligibleNodes) {
            System.out.println(node);
        }
    }

    public static void findAllCycles(MutableGraph<Path> graph) {
        List<Set<Path>> cycles = new ArrayList<>();
        Set<Path> visited = new HashSet<>();

        for (Path node : graph.nodes()) {
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

    private static void findCycles(MutableGraph<Path> graph, Path node, Set<Path> visited, Set<Path> currentPath, List<Path> currentCycle, List<Set<Path>> cycles) {
        visited.add(node);
        currentPath.add(node);
        currentCycle.add(node); // Track nodes in the current cycle

        for (Path neighbor : graph.successors(node)) {
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
