package com.github.slamdev.openapispringgenerator.plugin.validator;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.openapitools.codegen.validation.Invalid;
import org.openapitools.codegen.validation.ValidationResult;
import org.openapitools.codegen.validation.ValidationRule;
import org.openapitools.codegen.validations.oas.OpenApiEvaluator;
import org.openapitools.codegen.validations.oas.RuleConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Validator {

    public List<Invalid> validate(Path specFie) {
        List<Invalid> results = new ArrayList<>();

        String spec = readSpec(specFie);
        OpenApiEvaluator evaluator = new OpenApiEvaluator(new RuleConfiguration());
        SwaggerParseResult parserResult = new OpenAPIParser().readContents(spec, null, new ParseOptions());
        parserResult.getMessages().stream().map(m -> new Invalid(ValidationRule.error(m, null), m, null)).forEach(results::add);

        ValidationResult validationResult = evaluator.validate(parserResult.getOpenAPI());
        results.addAll(validationResult.getErrors());
        results.addAll(validationResult.getWarnings());

        return results;
    }

    private String readSpec(Path specFie) {
        try {
            return new String(Files.readAllBytes(specFie));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
