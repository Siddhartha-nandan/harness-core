/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.depanalyzer;

import io.harness.buildcleaner.MavenManifest;
import io.harness.buildcleaner.bazel.BuildFile;
import io.harness.buildcleaner.common.SymbolDependencyMap;
import io.harness.buildcleaner.javaparser.ClassMetadata;
import io.harness.buildcleaner.javaparser.ClasspathParser;
import io.harness.buildcleaner.javaparser.PackageParser;
import io.harness.buildcleaner.proto.ProtoBuildMapper;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import com.google.common.graph.MutableGraph;

@Slf4j
public class DependecyAnalyzer {
    private static final String BUILD_CLEANER_INDEX_FILE_NAME = ".build-cleaner-index";
    private CommandLine options;
    private MavenManifest mavenManifest;
    private MavenManifest mavenManifestOverride;
    private PackageParser packageParser;

    DependecyAnalyzer(String[] args) {
        this.options = getCommandLineOptions(args);

        Path mavenManifestFileLocation = mavenManifestFile();
        if (Files.exists(mavenManifestFileLocation)) {
            this.mavenManifest = MavenManifest.loadFromFile(mavenManifestFileLocation);
        }

        Path mavenManifestOverrideFileLocation = mavenManifestOverrideFile();
        if (Files.exists(mavenManifestOverrideFileLocation)) {
            this.mavenManifestOverride = MavenManifest.loadFromOverrideFile(mavenManifestOverrideFileLocation);
        }

        this.packageParser = new PackageParser(workspace());
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new DependecyAnalyzer(args).run();
    }

    public void run() throws IOException, ClassNotFoundException {
        log.info("Workspace: " + workspace());

        // Create harness code index or load from an existing index.
        SymbolDependencyMap harnessSymbolMap = buildHarnessSymbolMap();
        log.debug("Total Java classes found: " + harnessSymbolMap.getCacheSize());

//        Path dirPath = Paths.get("950-delegate-tasks-beans/src/main/java/io/harness/connector/helper");
        printModuleDependencies(module(), harnessSymbolMap);

        DirectoryGraphBuilder graphBuilder = new DirectoryGraphBuilder();
        MutableGraph<Path> graph = graphBuilder.getGraph();


        // Create Graph
        buildGraph(graph, module(), harnessSymbolMap);

        GraphUtils.printIndependentPackages(graph, module());

//        GraphUtils.printDisconnectedComponents(graph);
//
//        GraphUtils.findAllCycles(graph);
    }

    public void buildGraph(MutableGraph<Path> graph, Path sourceDirectory, SymbolDependencyMap harnessSymbolMap) throws IOException {
        buildGraphRecursive(graph, sourceDirectory, harnessSymbolMap);
    }

    private void buildGraphRecursive(MutableGraph<Path> graph, Path path, SymbolDependencyMap harnessSymbolMap) throws IOException {
        if (!Files.isDirectory(workspace().resolve(path))) {
            return;
        }
        Set<String> sourceFiles = getSourceFiles(workspace().resolve(path));
        if (sourceFiles.isEmpty()) {
            log.warn("No sources found for {}", path);
        }
        else {
            // Add the node if it's not already in the graph
            graph.addNode(path);

            ClasspathParser classpathParser = this.packageParser.getClassPathParser();
            String parseClassPattern = path.toString().isEmpty() ? srcsGlob() : path + "/" + srcsGlob();
            classpathParser.parseClasses(parseClassPattern, new HashSet<>());

            // Get dependencies and add edges
            Set<String> dependencies = getModuleDependencies(path, classpathParser, harnessSymbolMap);
            for (String destination : dependencies) {
                Path destinationPath = Paths.get(destination);
                if(destinationPath.equals(path)){
                    continue;
                }
                if (!graph.nodes().contains(destinationPath)) {
                    graph.addNode(destinationPath);
                }
                graph.putEdge(path, destinationPath);
            }
        }

        // Traverse subdirectories
        Files
            .list(workspace().resolve(path)).filter(Files::isDirectory)
            .forEach(dirPath -> {
                try {
                    buildGraphRecursive(graph, workspace().relativize(dirPath), harnessSymbolMap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
    /**
     * Creates symbol to package path index using package parser. If an index file already exists, directly
     * load it.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @VisibleForTesting
    protected SymbolDependencyMap buildHarnessSymbolMap() throws IOException, ClassNotFoundException {
        final var harnessSymbolMap = initDependencyMap();

        // if symbol map exists and no options specified then don't update it
        if (!harnessSymbolMap.getSymbolToTargetMap().isEmpty() && !options.hasOption("indexSourceGlob")) {
            return harnessSymbolMap;
        }

        log.info("Creating index using sources matching: {}", indexSourceGlob());

        // Parse proto and BUILD files to construct Proto specific java symbols to proto target map.
        final ProtoBuildMapper protoBuildMapper = new ProtoBuildMapper(workspace());
        protoBuildMapper.protoToBuildTargetDependencyMap(indexSourceGlob(), harnessSymbolMap);

        // Parse java classes.
        final ClasspathParser classpathParser = packageParser.getClassPathParser();
        classpathParser.parseClasses(indexSourceGlob(), assumedPackagePrefixesWithBuildFile());

        // Update symbol dependency map with the parsed java code.
        final Set<ClassMetadata> fullyQualifiedClassNames = classpathParser.getFullyQualifiedClassNames();
        for (ClassMetadata metadata : fullyQualifiedClassNames) {
            harnessSymbolMap.addSymbolTarget(metadata.getFullyQualifiedClassName(), metadata.getBuildModulePath());
        }

        harnessSymbolMap.serializeToFile(indexFilePath().toString());
        log.info("Index creation complete.");

        return harnessSymbolMap;
    }

    /**
     * If user wants to override index partially to make the scan faster (e.g. changed just couple of modules)
     * they can include <b>indexSourcesGlob</b> option to specify just certain packages to be scanned
     * <pre>--indexSourceGlob {260-delegate-service,980-commons}/src&#47;**&#47;*"}</pre>
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @NonNull
    private SymbolDependencyMap initDependencyMap() throws IOException, ClassNotFoundException {
        if (indexFileExists() && !options.hasOption("overrideIndex")) {
            log.info("Loading the existing index file {} to init dependency map", indexFilePath());
            return SymbolDependencyMap.deserializeFromFile(indexFilePath().toString());
        } else {
            return new SymbolDependencyMap();
        }
    }

    /**
     * Helper method to resolve the import statement to get
     * the dependencies for the module using the symbol map.
     *
     * @param path relative to the workspace.
     * @param classpathParser classpath parser to get imports for java files.
     * @param harnessSymbolMap having mapping from Harness classes to build paths.
     * @return dependencies for the module as a set.
     */
    private Set<String> getModuleDependencies(
            Path path, ClasspathParser classpathParser, SymbolDependencyMap harnessSymbolMap) {
        Set<String> dependencies = new TreeSet<>();
        for (String importStatement : classpathParser.getUsedTypes()) {
            if (importStatement.startsWith("java.")) {
                continue;
            }

            Optional<String> resolvedSymbol = resolve(importStatement, harnessSymbolMap);
            // Skip the symbols from the same package. Resolved symbol starts with "//" and ends with ":module" and rest of
            // it is just a path - therefore, removing first two characters before comparing.
            if (resolvedSymbol.isPresent()
                    && resolvedSymbol.get().substring(2, resolvedSymbol.get().length() - 7).equals(path.toString())) {
                continue;
            }

            resolvedSymbol.ifPresent(symbol -> log.debug("Adding dependency to {} for import {}", symbol, importStatement));
            resolvedSymbol.ifPresent(dependencies::add);
            if (resolvedSymbol.isEmpty()) {
                log.error("No build dependency found for {}", importStatement);
            }
        }

        return dependencies;
    }

    public void printModuleDependencies(Path path, SymbolDependencyMap harnessSymbolMap) throws IOException, FileNotFoundException{
        ClasspathParser classpathParser = this.packageParser.getClassPathParser();
        String parseClassPattern = path.toString().isEmpty() ? srcsGlob() : path + "/" + srcsGlob();
        classpathParser.parseClasses(parseClassPattern, new HashSet<>());

        Set<String> dependencies = getModuleDependencies(path, classpathParser, harnessSymbolMap);

        for (String dep: dependencies){
            System.out.println(dep);
        }
    }

    /**
     * Find the dependency to include for the import statement.
     * @param importStatement to resolve.
     * @param harnessSymbolMap containing java symbols to build dependency map.
     * @return Optional build target name which has the import symbol.
     */
    private Optional<String> resolve(String importStatement, SymbolDependencyMap harnessSymbolMap) {
        Optional<String> resolvedSymbol = Optional.empty();

        if (mavenManifestOverride != null) {
            resolvedSymbol = mavenManifestOverride.getTarget(importStatement);
            if (resolvedSymbol.isPresent()) {
                return resolvedSymbol;
            }
        }

        // Look up symbol in the harness symbol map.
        resolvedSymbol = harnessSymbolMap.getTarget(importStatement);
        if (resolvedSymbol.isPresent()) {
            // For Java targets, we don't have java_library name in the Symbol dependency map.
            if (!resolvedSymbol.get().contains(":")) {
//                return Optional.of(String.format("//%s:%s", resolvedSymbol.get(), DEFAULT_JAVA_LIBRARY_NAME));
                return Optional.of(String.format(resolvedSymbol.get()));
            }
            return resolvedSymbol;
        }

        // Look up symbol in the maven manifest.
        if (mavenManifest != null) {
            resolvedSymbol = mavenManifest.getTarget(importStatement);
            if (resolvedSymbol.isPresent() && !resolvedSymbol.get().equals("@maven//:io_harness_ng_license_beans")) {
                return resolvedSymbol;
            }
        }

        return Optional.empty();
    }

    /**
     * Finds java source files present in the directory.
     * @param directory to search.
     * @return Set of java files in the input folder.
     * @throws IOException
     */
    private Set<String> getSourceFiles(Path directory) throws IOException {
        final var syntaxAndPattern = "glob:" + directory + "/" + srcsGlob();
        log.info("Scanning for files using pattern {}", syntaxAndPattern);
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
        Set<String> sourceFileNames = new HashSet<>();
        try (Stream<Path> paths = Files.find(directory, Integer.MAX_VALUE, (path, f) -> pathMatcher.matches(path))) {
            paths.forEach(path -> sourceFileNames.add(path.getFileName().toString()));
        }
        return sourceFileNames;
    }

    private boolean indexFileExists() {
        File f = new File(indexFilePath().toString());
        return f.exists();
    }

    private Path indexFilePath() {
        return options.hasOption("indexFile") ? Paths.get(options.getOptionValue("indexFile"))
                : workspace().resolve(BUILD_CLEANER_INDEX_FILE_NAME);
    }

    private Path workspace() {
        return options.hasOption("workspace") ? Paths.get(options.getOptionValue("workspace")) : Paths.get("");
    }

    private Path module() {
        return options.hasOption("module") ? Paths.get(options.getOptionValue("module")) : Paths.get("");
    }

    private String srcsGlob() {
        return options.hasOption("srcsGlob") ? options.getOptionValue("srcsGlob") : "*.java";
    }

    private Set<String> assumedPackagePrefixesWithBuildFile() {
        return options.hasOption("assumedPackagePrefixesWithBuildFile")
                ? new HashSet<String>(Arrays.asList(options.getOptionValue("assumedPackagePrefixesWithBuildFile").split(",")))
                : new HashSet<String>();
    }

    private Path mavenManifestFile() {
        return options.hasOption("mavenManifestFile") ? Paths.get(options.getOptionValue("mavenManifestFile"))
                : Paths.get(workspace() + "/maven-manifest.json");
    }

    private Path mavenManifestOverrideFile() {
        return options.hasOption("mavenManifestOverrideFile")
                ? Paths.get(options.getOptionValue("mavenManifestOverrideFile"))
                : Paths.get(workspace() + "/maven-manifest-override.txt");
    }

    private String indexSourceGlob() {
        return options.hasOption("indexSourceGlob") ? options.getOptionValue("indexSourceGlob") : "**/src/**/*";
    }
    private CommandLine getCommandLineOptions(String[] args) {
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(new Option(null, "workspace", true, "Workspace root"));
        options.addOption(
                new Option(null, "indexSourceGlob", true, "Pattern for source files to build index. Defaults to '**/src/**/*"));
        options.addOption(new Option(null, "overrideIndex", false, "Override the existing index"));
        options.addOption(new Option(null, "module", true, "Relative path of the module from the workspace"));
        options.addOption(new Option(null, "srcsGlob", true, "Pattern to match for finding source files."));
        options.addOption(
                new Option(null, "indexFile", true, "Index file having cache, defaults to $workspace/.build-cleaner-path-index"));
        options.addOption(new Option(null, "mavenManifestFile", true,
                "Absolute path of the manifest file having java package to maven target mapping."
                        + "Defaults to workspace/maven_manifest.json"));
        options.addOption(
                new Option(null, "mavenManifestOverrideFile", true, "Specify overrides for conflicting imports."));
        options.addOption(new Option(null, "assumedPackagePrefixesWithBuildFile", true,
                "Comma separate list of module prefixes for which we can assume BUILD file to be present. "
                        + "Set to 'all' if need same behavior for all folders"));
        CommandLine commandLineOptions = null;
        try {
            commandLineOptions = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Command line parsing failed. {}", e.getMessage());
            System.exit(3);
        }
        return commandLineOptions;
    }
};
