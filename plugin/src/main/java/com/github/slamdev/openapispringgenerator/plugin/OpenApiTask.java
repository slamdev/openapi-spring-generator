package com.github.slamdev.openapispringgenerator.plugin;

import com.github.slamdev.openapispringgenerator.lib.generator.SpringCodegen;
import com.github.slamdev.openapispringgenerator.lib.validator.Validator;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.*;
import org.gradle.internal.logging.progress.ProgressLogger;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.progress.PercentageProgressFormatter;
import org.openapitools.codegen.validation.Invalid;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
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
        specs.add(toSpec(SpringCodegen.Type.CLIENT, file));
    }

    public void client(FileCollection fileCollection) {
        specs.addAll(toSpecs(SpringCodegen.Type.CLIENT, fileCollection));
    }

    public void server(File file) {
        specs.add(toSpec(SpringCodegen.Type.SERVER, file));
    }

    public void server(FileCollection fileCollection) {
        specs.addAll(toSpecs(SpringCodegen.Type.SERVER, fileCollection));
    }

    public void producer(File file) {
        specs.add(toSpec(SpringCodegen.Type.PRODUCER, file));
    }

    public void producer(FileCollection fileCollection) {
        specs.addAll(toSpecs(SpringCodegen.Type.PRODUCER, fileCollection));
    }

    public void consumer(File file) {
        specs.add(toSpec(SpringCodegen.Type.CONSUMER, file));
    }

    public void consumer(FileCollection fileCollection) {
        specs.addAll(toSpecs(SpringCodegen.Type.CONSUMER, fileCollection));
    }

    private Spec toSpec(SpringCodegen.Type type, File file) {
        return new Spec(file.toPath(), type);
    }

    private List<Spec> toSpecs(SpringCodegen.Type type, FileCollection fileCollection) {
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
        boolean failed = false;
        try {
            for (Spec spec : specs) {
                progressLogger.progress("Validating '" + spec.file.getFileName() + "' spec for " + spec.getType());
                List<Invalid> results = new Validator().validate(spec.file);
                if (!results.isEmpty()) {
                    failed = true;
                }
                for (Invalid result : results) {
                    getLogger().error("{}: {}", result.getRule().getSeverity(), result.getMessage());
                }
            }
        } finally {
            progressLogger.completed("Validation completed", failed);
        }

        progressLogger = progressLoggerFactory.newOperation(OpenApiTask.class);
        progressLogger.start("OpenApi code generation", null);
        try {
            PercentageProgressFormatter progressFormatter = new PercentageProgressFormatter("Generating", specs.size());
            for (Spec spec : specs) {
                progressLogger.progress(progressFormatter.getProgress());
                progressLogger.progress("Generating '" + spec.file.getFileName() + "' spec for " + spec.getType());
                generate(spec.getFile(), spec.getType(), tempDir, useOptional);

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
                    if (!"org.springframework.boot.autoconfigure.AutoConfiguration.imports".equals(src.getFileName().toString())) {
                        throw new IllegalStateException("" + dest + " already exists");
                    }
                    mergeSpringFactories(src, dest);
                });

                progressFormatter.increment();
            }
        } finally {
            progressLogger.completed("Generation completed", false);
        }
    }

    private void generate(Path specFie, SpringCodegen.Type type, Path outputDir, boolean useOptional) {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setLang(SpringCodegen.class.getName());
        configurator.setLibrary(type.name().toLowerCase(Locale.ROOT));
        configurator.setOutputDir(outputDir.toString());
        configurator.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, useOptional);
        try {
            configurator.setInputSpec(new String(Files.readAllBytes(specFie)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(configurator.toClientOptInput());
        generator.setGenerateSwaggerMetadata(false);
        generator.generate();
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
            byte[] srcContent = Files.readAllBytes(src);
            Files.write(dest, srcContent, StandardOpenOption.APPEND);
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
        private final SpringCodegen.Type type;

        private Spec(Path file, SpringCodegen.Type type) {
            this.file = file;
            this.type = type;
        }

        public Path getFile() {
            return file;
        }

        public SpringCodegen.Type getType() {
            return type;
        }
    }
}
