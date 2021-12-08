package com.github.slamdev.openapispringgenerator.cli;

import com.github.slamdev.openapispringgenerator.lib.generator.OpenAPIYamlCodegen;
import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.codegen.v3.generators.openapi.OpenAPIGenerator;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(name = "convert", mixinStandardHelpOptions = true, description = "convert to OpenAPI v3")
public class Convert implements Runnable {

    @CommandLine.Option(names = {"-f", "--input-file"}, required = true, description = "path to a spec file")
    private Path specFile;

    @CommandLine.Option(names = {"-o", "--output-file"}, required = true, description = "path to store the generated file")
    private Path outputFile;

    @Override
    public void run() {
        Path abs = outputFile.toAbsolutePath();
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setLang(OpenAPIYamlCodegen.class.getName());
        configurator.setOutputDir(abs.getParent().toString());
        configurator.addAdditionalProperty(OpenAPIGenerator.OUTPUT_NAME, abs.getFileName().toString());
        configurator.addAdditionalProperty(OpenAPIGenerator.FLATTEN_SPEC, true);
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
