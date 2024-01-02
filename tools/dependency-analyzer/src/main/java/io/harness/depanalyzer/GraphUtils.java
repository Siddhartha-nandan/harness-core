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
            System.out.println("Node " + node.toString() + " : ");
            for (Path successor : graph.successors(node)) {
                System.out.println("    Depends on: " + successor);
            }
            for (Path predecessor : graph.predecessors(node)) {
                System.out.println("    Dependency of: " + predecessor);
            }
            System.out.println();
        }
        System.out.println("Number of packages: " + graph.nodes().size());
    }

    public static void printWeaklyConnectedComponents(MutableGraph<Path> graph) {
        Set<Path> visited = new HashSet<>();
        int componentId = 0;

        for (Path node : graph.nodes()) {
            if (!visited.contains(node)) {
                componentId++;
                System.out.println("Weakly Connected Component " + componentId + ":");
                exploreComponent(graph, node, visited);
                System.out.println();
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
                System.out.println("  Node: " + node);

                // Print successors of the node
                for (Path successor : graph.successors(node)) {
                    System.out.println("    Successor: " + successor);
                    if (!visited.contains(successor)) {
                        stack.push(successor);
                    }
                }

                // Consider predecessors to simulate undirected edges
                for (Path predecessor : graph.predecessors(node)) {
                    System.out.println("    Predecessor: " + predecessor);
                    if (!visited.contains(predecessor)) {
                        stack.push(predecessor);
                    }
                }
            }
        }
    }

    public static void printNodesByLevel(MutableGraph<String> graph) {
        // Implementation using Guava's graph traversal methods or iterators
    }

    public static void printIndependentPackages(MutableGraph<Path> graph, Path filterDirectory) {
        System.out.println("Independent Packages:");
        Set<Path> nodesInDirectory = new HashSet<>();
        for (Path node : graph.nodes()) {
            if (node.startsWith(filterDirectory)) {
                nodesInDirectory.add(node);
            }
        }

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

        // Print sorted node
        for (Path node : eligibleNodes) {
            System.out.println(node);
        }
    }

    public static void printCycles(MutableGraph<Path> graph) {
        Set<Path> visited = new HashSet<>();
        Set<Path> recStack = new HashSet<>();
        Stack<Path> pathStack = new Stack<>();

        for (Path node : graph.nodes()) {
            if (detectCycle(graph, node, visited, recStack, pathStack)) {
                System.out.println("Cycle detected: ");
                while (!pathStack.isEmpty()) {
                    System.out.print(pathStack.pop() + " ");
                }
                System.out.println();
            }
        }
    }

    private static boolean detectCycle(MutableGraph<Path> graph, Path node, Set<Path> visited, Set<Path> recStack, Stack<Path> pathStack) {
        if (recStack.contains(node)) {
            pathStack.push(node);
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recStack.add(node);
        pathStack.push(node);

        for (Path neighbor : graph.successors(node)) {
            if (detectCycle(graph, neighbor, visited, recStack, pathStack)) {
                return true;
            }
        }

        recStack.remove(node);
        pathStack.pop();
        return false;
    }


    private static String quote(String str) {
        // Add quotes if the string contains special characters or spaces
        if (str.matches(".*[^a-zA-Z0-9_].*")) {
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        return str;
    }

    public static void exportGraphToDot(MutableGraph<Path> graph, Path filename) throws IOException {
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
        Files.writeString(filename, dotString.toString());
    }

    public static void printNodesWithSpecificSuccessors(MutableGraph<Path> graph, Set<String> allowedNodesSet) {
        Set<Path> allowedNodes = new HashSet<>();
        for (String nodeStr : allowedNodesSet) {
            allowedNodes.add(Paths.get(nodeStr));
        }
        for (Path node : graph.nodes()) {
            if (allowedNodes.contains(node)) {
                continue;
            }

            boolean allSuccessorsAllowed = true;

            for (Path successor : graph.successors(node)) {
                if (!allowedNodes.contains(successor)) {
                    allSuccessorsAllowed = false;
                    break;
                }
            }

            if (allSuccessorsAllowed) {
                System.out.println(node);
            }
        }
    }
}
