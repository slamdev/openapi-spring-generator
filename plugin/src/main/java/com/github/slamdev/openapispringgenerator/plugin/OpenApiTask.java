package com.github.slamdev.openapispringgenerator.plugin;

import com.github.slamdev.openapispringgenerator.generator.Generator;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.*;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.progress.PercentageProgressFormatter;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class OpenApiTask extends DefaultTask {

    private static class Spec {
        private final Path file;
        private final Generator.Type type;

        private Spec(Path file, Generator.Type type) {
            this.file = file;
            this.type = type;
        }

        public Path getFile() {
            return file;
        }

        public Generator.Type getType() {
            return type;
        }
    }

    private final List<Spec> specs = new ArrayList<>();

    private final ProgressLoggerFactory progressLoggerFactory;

    private File destinationDir;

    @Inject
    public OpenApiTask(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLoggerFactory = progressLoggerFactory;
    }

    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    public void client(File file) {
        specs.add(new Spec(file.toPath(), Generator.Type.CLIENT));
    }

    public void client(FileCollection fileCollection) {
        fileCollection.getFiles().stream()
                .map(File::toPath)
                .map(f -> new Spec(f, Generator.Type.CLIENT))
                .forEach(specs::add);
    }

    public void server(File file) {
        specs.add(new Spec(file.toPath(), Generator.Type.SERVER));
    }

    public void server(FileCollection fileCollection) {
        fileCollection.getFiles().stream()
                .map(File::toPath)
                .map(f -> new Spec(f, Generator.Type.SERVER))
                .forEach(specs::add);
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public List<File> getSpecs() {
        return specs.stream()
                .map(Spec::getFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    @TaskAction
    public void generate() throws IOException {
        Path tempDir = getTemporaryDir().toPath();
        clearDir(tempDir);

        clearDir(destinationDir.toPath());

        ProgressLogger progressLogger = progressLoggerFactory.newOperation(OpenApiTask.class);
        progressLogger.start("OpenApi code generation", null);
        try {
            PercentageProgressFormatter progressFormatter = new PercentageProgressFormatter("Generating", specs.size());
            for (Spec spec : specs) {
                progressLogger.progress(progressFormatter.getProgress());
                new Generator().generate(spec.getFile(), spec.getType(), tempDir);

                FileTree javaTree = (FileTree) getProject().fileTree(tempDir).include("**/*.java");
                move(javaTree, "java", (src, dest) -> {
                    throw new IllegalStateException("" + dest + " already exists");
                });

                FileTree resourcesTree = (FileTree) getProject().fileTree(tempDir).exclude("**/*.java");
                move(resourcesTree, "resources", (src, dest) -> {
                    if (!src.getFileName().toString().equals("spring.factories")) {
                        throw new IllegalStateException("" + dest + " already exists");
                    }
                    mergeSpringFactories(src, dest);
                });

                progressFormatter.increment();
            }
        } finally {
            progressLogger.completed();
        }
    }

    private void clearDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void mergeSpringFactories(Path src, Path dest) {
        try {
            Properties srcProps = new Properties();
            srcProps.load(Files.newBufferedReader(src));

            Properties destProps = new Properties();
            destProps.load(Files.newBufferedReader(dest));

            for (String key : srcProps.stringPropertyNames()) {
                if (destProps.containsKey(key)) {
                    String merged = destProps.getProperty(key) + "," + srcProps.getProperty(key);
                    destProps.put(key, merged);
                } else {
                    destProps.put(key, srcProps.getProperty(key));
                }
            }

            destProps.store(Files.newBufferedWriter(dest, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING), null);

            Files.delete(src);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void move(FileTree tree, String dir, BiConsumer<Path, Path> merge) throws IOException {
        Path newDir = destinationDir.toPath().resolve("main").resolve(dir);
        for (File file : tree.getFiles()) {
            Path fileName = getTemporaryDir().toPath().relativize(file.toPath());
            Path newFile = newDir.resolve(fileName);
            if (!Files.exists(newFile.getParent())) {
                Files.createDirectories(newFile.getParent());
            }
            if (Files.exists(newFile)) {
                merge.accept(file.toPath(), newFile);
            } else {
                Files.move(file.toPath(), newFile);
            }
        }
    }
}
