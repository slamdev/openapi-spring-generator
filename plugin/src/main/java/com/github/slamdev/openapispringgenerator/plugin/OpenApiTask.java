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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OpenApiTask extends DefaultTask {

    private final List<List<File>> clients = new ArrayList<>();

    private final List<List<File>> servers = new ArrayList<>();

    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    private File destinationDir;

    private final ProgressLoggerFactory progressLoggerFactory;

    @Inject
    public OpenApiTask(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLoggerFactory = progressLoggerFactory;
    }

    public void client(File file) {
        clients.add(Collections.singletonList(file));
    }

    public void client(FileCollection fileCollection) {
        clients.add(new ArrayList<>(fileCollection.getFiles()));
    }

    public void server(File file) {
        servers.add(Collections.singletonList(file));
    }

    public void server(FileCollection fileCollection) {
        servers.add(new ArrayList<>(fileCollection.getFiles()));
    }

    @Internal
    protected List<File> getSpecs() {
        List<File> all = new ArrayList<>();
        clients.stream().flatMap(List::stream).forEach(all::add);
        servers.stream().flatMap(List::stream).forEach(all::add);
        return all;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    protected List<File> getClientSpecs() {
        return clients.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    protected List<File> getServerSpecs() {
        return servers.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private void move(FileTree tree, String dir) {
        File tempDir = new File(destinationDir, "temp");
        Path newDir = destinationDir.toPath().resolve("main").resolve(dir);
        for (File file : tree.getFiles()) {
            Path fileName = tempDir.toPath().relativize(file.toPath());
            Path newFile = newDir.resolve(fileName);
            try {
                if (!Files.exists(newFile.getParent())) {
                    Files.createDirectories(newFile.getParent());
                }
                Files.move(file.toPath(), newFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @TaskAction
    public void generate() {
        ProgressLogger progressLogger = progressLoggerFactory.newOperation(OpenApiTask.class);
        progressLogger.start("Swagger code generation", null);

        try {
            PercentageProgressFormatter progressFormatter = new PercentageProgressFormatter("Generating", getSpecs().size() + 2);
            progressLogger.progress(progressFormatter.incrementAndGetProgress());
            delete(destinationDir);
            progressLogger.progress(progressFormatter.incrementAndGetProgress());

            File tempDir = new File(destinationDir, "temp");

            tempDir.mkdirs();

            servers.forEach(files -> {
                if (files.isEmpty()) {
                    return;
                }
                progressLogger.progress(progressFormatter.getProgress());
                files.stream()
                        .map(File::toPath)
                        .forEach(specFile -> new Generator().generate(specFile, Generator.Type.SERVER, tempDir.toPath()));
                progressFormatter.increment();
            });
            clients.forEach(files -> {
                if (files.isEmpty()) {
                    return;
                }
                progressLogger.progress(progressFormatter.getProgress());
                files.stream()
                        .map(File::toPath)
                        .forEach(specFile -> new Generator().generate(specFile, Generator.Type.CLIENT, tempDir.toPath()));
                progressFormatter.increment();
            });
            FileTree javaTree = (FileTree) getProject().fileTree(tempDir).include("**/*.java");
            move(javaTree, "java");
            FileTree resourcesTree = (FileTree) getProject().fileTree(tempDir).exclude("**/*.java");
            move(resourcesTree, "resources");
            delete(tempDir);
        } finally {
            progressLogger.completed();
        }
    }

    private void delete(File f) {
        Path file = f.toPath();
        if (!Files.exists(file)) {
            return;
        }
        try {
            if (Files.isDirectory(file)) {
                Files.walk(file)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
