package com.github.slamdev.openapispringgenerator.plugin;

import com.github.slamdev.openapispringgenerator.plugin.generator.Generator;
import com.github.slamdev.openapispringgenerator.plugin.validator.Validator;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.*;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.progress.PercentageProgressFormatter;
import org.openapitools.codegen.validation.Invalid;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class OpenApiTask extends DefaultTask {

    private boolean useOptional = true;

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
        specs.add(toSpec(Generator.Type.CLIENT, file));
    }

    public void client(FileCollection fileCollection) {
        specs.addAll(toSpecs(Generator.Type.CLIENT, fileCollection));
    }

    public void server(File file) {
        specs.add(toSpec(Generator.Type.SERVER, file));
    }

    public void server(FileCollection fileCollection) {
        specs.addAll(toSpecs(Generator.Type.SERVER, fileCollection));
    }

    public void producer(File file) {
        specs.add(toSpec(Generator.Type.PRODUCER, file));
    }

    public void producer(FileCollection fileCollection) {
        specs.addAll(toSpecs(Generator.Type.PRODUCER, fileCollection));
    }

    public void consumer(File file) {
        specs.add(toSpec(Generator.Type.CONSUMER, file));
    }

    public void consumer(FileCollection fileCollection) {
        specs.addAll(toSpecs(Generator.Type.CONSUMER, fileCollection));
    }

    private Spec toSpec(Generator.Type type, File file) {
        return new Spec(file.toPath(), type);
    }

    private List<Spec> toSpecs(Generator.Type type, FileCollection fileCollection) {
        return fileCollection.getFiles().stream()
                .map(File::toPath)
                .map(f -> new Spec(f, type))
                .collect(Collectors.toList());
    }

    @Input
    public boolean isUseOptional() {
        return useOptional;
    }

    public void setUseOptional(boolean useOptional) {
        this.useOptional = useOptional;
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
    public void run() throws IOException {
        Path tempDir = getTemporaryDir().toPath();
        clearDir(tempDir);
        clearDir(destinationDir.toPath());

        ProgressLogger progressLogger = progressLoggerFactory.newOperation(OpenApiTask.class);
        progressLogger.start("OpenApi spec validation", null);
        try {
            for (Spec spec : specs) {
                getLogger().lifecycle("Validating '{}' spec for {}", spec.file.getFileName(), spec.getType());
                List<Invalid> results = new Validator().validate(spec.file);
                for (Invalid result : results) {
                    getLogger().warn("{}: {}", result.getRule().getSeverity(), result.getMessage());
                }
            }
        } finally {
            progressLogger.completed();
        }

        progressLogger = progressLoggerFactory.newOperation(OpenApiTask.class);
        progressLogger.start("OpenApi code generation", null);
        try {
            PercentageProgressFormatter progressFormatter = new PercentageProgressFormatter("Generating", specs.size());
            for (Spec spec : specs) {
                progressLogger.progress(progressFormatter.getProgress());
                getLogger().lifecycle("Generating '{}' spec for {}", spec.file.getFileName(), spec.getType());
                new Generator().generate(spec.getFile(), spec.getType(), tempDir, useOptional);

                FileTree javaTree = (FileTree) getProject().fileTree(tempDir).include("**/*.java");
                move(javaTree, "java", (src, dest) -> {
                    try {
                        byte[] f1 = Files.readAllBytes(src);
                        byte[] f2 = Files.readAllBytes(dest);
                        if (Arrays.equals(f1, f2)) {
                            return;
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                    throw new IllegalStateException("" + dest + " already exists");
                });

                FileTree resourcesTree = (FileTree) getProject().fileTree(tempDir).exclude("**/*.java");
                move(resourcesTree, "resources", (src, dest) -> {
                    if (!"spring.factories".equals(src.getFileName().toString())) {
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
        try (BufferedReader srcReader = Files.newBufferedReader(src); BufferedReader destReader = Files.newBufferedReader(dest)) {
            Properties srcProps = new Properties();
            srcProps.load(srcReader);

            Properties destProps = new Properties();
            destProps.load(destReader);

            for (String key : srcProps.stringPropertyNames()) {
                if (destProps.containsKey(key)) {
                    String merged = destProps.getProperty(key) + "," + srcProps.getProperty(key);
                    destProps.put(key, merged);
                } else {
                    destProps.put(key, srcProps.getProperty(key));
                }
            }

            try (BufferedWriter writer = Files.newBufferedWriter(dest, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                destProps.store(writer, null);
            }
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
}
