package com.github.slamdev.openapispringgenerator.generator;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Generator {

    public enum Type {
        SERVER, CLIENT
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    public void generate(Path specFie, Type type, Path outputDir, boolean useOptional) {
        LOGGER.info("generating {} code for {} in {}", type, specFie, outputDir);

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
        ClientOptInput input = configurator.toClientOptInput();

        List<File> files = new DefaultGenerator().opts(input).generate();

        safeDelete(outputDir.resolve(".swagger-codegen-ignore"));
        safeDelete(outputDir.resolve(".swagger-codegen"));

        Formatter formatter = new Formatter();

        files.stream()
                .map(File::toPath)
                .filter(f -> f.getFileName().toString().endsWith(".java"))
                .forEach(f -> this.format(formatter, f));
    }

    private void format(Formatter formatter, Path file) {
        try {
            String str = new String(Files.readAllBytes(file));
            str = formatter.formatSource(str);
            Files.write(file, str.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException | FormatterException e) {
            LOGGER.error("failed to format " + file, e);
        }
    }

    private void safeDelete(Path file) {
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
            LOGGER.error("failed to delete file " + file, e);
        }
    }
}
