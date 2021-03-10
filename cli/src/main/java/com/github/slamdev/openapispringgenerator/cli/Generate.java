package com.github.slamdev.openapispringgenerator.cli;

import com.github.slamdev.openapispringgenerator.lib.generator.SpringCodegen;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@CommandLine.Command(name = "generate", mixinStandardHelpOptions = true, description = "generate java code")
public class Generate implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"}, required = true, description = "path to a spec file")
    private Path specFile;

    @CommandLine.Option(names = {"-t", "--type"}, required = true, description = "library type")
    private SpringCodegen.Type type;

    @CommandLine.Option(names = {"-o", "--output"}, required = true, description = "path to store the generated code")
    private Path outputDir;

    @CommandLine.Option(names = {"--use-optional"}, description = "use java.util.Optional for parameters signature")
    private boolean useOptional;

    @Override
    public void run() {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setLang(SpringCodegen.class.getName());
        configurator.setLibrary(type.name().toLowerCase(Locale.ROOT));
        configurator.setOutputDir(outputDir.toString());
        configurator.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, useOptional);
        try {
            configurator.setInputSpec(new String(Files.readAllBytes(specFile)));
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
