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
        Set<Path> visited = new HashSet<>();
        int componentId = 0;

        for (Path node : graph.nodes()) {
            if (!visited.contains(node)) {
                componentId++;
                System.out.println("Component " + componentId + ":");
                exploreComponent(graph, node, visited);
                System.out.println(); // New line after each component
            }
        }
    }

    private static void exploreComponent(MutableGraph<Path> graph, Path start, Set<Path> visited) {
        Stack<Path> stack = new Stack<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            Path node = stack.pop();

            if (!visited.contains(node)) {
                visited.add(node);
                System.out.println("  Node: " + node); // Print node in the component

                for (Path neighbor : graph.successors(node)) {
                    System.out.println("    Edge: " + node + " -> " + neighbor); // Print edge
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
        }
    }

    public static void printNodesByLevel(MutableGraph<String> graph) {
        // Implementation using Guava's graph traversal methods or iterators
        // (implementation not shown for brevity)
    }

    public static void printIndependentPackages(MutableGraph<Path> graph, Path filterDirectory) {
        System.out.println("Independent Packages:");
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

    private static String quote(String str) {
        // Add quotes if the string contains special characters or spaces
        if (str.matches(".*[^a-zA-Z0-9_].*")) {
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        return str;
    }

    public static void exportGraphToDot(MutableGraph<Path> graph, String filename) throws IOException {
        StringBuilder dotString = new StringBuilder("digraph G {\n");

        // Add nodes
        for (Path node : graph.nodes()) {
            String safeNode = quote(node.toString());
            dotString.append("  ").append(safeNode).append(";\n");
        }

        // Add edges
        for (EndpointPair<Path> edge : graph.edges()) {
            String nodeU = quote(edge.nodeU().toString());
            String nodeV = quote(edge.nodeV().toString());
            dotString.append("  ").append(nodeU).append(" -> ").append(nodeV).append(";\n");
        }

        dotString.append("}\n");

        // Write DOT string to file
        Files.writeString(Paths.get(filename), dotString.toString());
    }

}
