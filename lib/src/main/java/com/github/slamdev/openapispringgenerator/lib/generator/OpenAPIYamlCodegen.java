package com.github.slamdev.openapispringgenerator.lib.generator;

import io.swagger.codegen.v3.generators.openapi.OpenAPIYamlGenerator;

public class OpenAPIYamlCodegen extends OpenAPIYamlGenerator {

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.clear();
    }
}
