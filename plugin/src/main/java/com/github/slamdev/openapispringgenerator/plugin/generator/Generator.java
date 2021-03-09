package com.github.slamdev.openapispringgenerator.plugin.generator;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class Generator {

    public enum Type {
        SERVER, CLIENT, CONSUMER, PRODUCER
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

        DefaultGenerator generator = new DefaultGenerator();
        generator.opts(input);
        generator.setGenerateSwaggerMetadata(false);
        generator.generate();
    }
}
