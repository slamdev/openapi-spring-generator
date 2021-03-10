package com.github.slamdev.openapispringgenerator.lib.generator;

import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GeneratorTest {

    @Test
    public void should_generate() {
        generate(file("event-spec-v3.yaml"), "consumer", Paths.get("build/test-output"), true);
    }

    private Path file(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(name).getFile()).toPath();
    }

    private void generate(Path specFie, String type, Path outputDir, boolean useOptional) {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setLang(SpringCodegen.class.getName());
        configurator.setLibrary(type);
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
}
