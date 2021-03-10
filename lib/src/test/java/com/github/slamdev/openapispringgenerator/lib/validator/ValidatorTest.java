package com.github.slamdev.openapispringgenerator.lib.validator;

import org.junit.Test;
import org.openapitools.codegen.validation.Invalid;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class ValidatorTest {

    private static final Logger LOGGER = Logger.getLogger(ValidatorTest.class.getName());

    @Test
    public void should_validate() {
        Validator v = new Validator();
        List<Invalid> results = v.validate(file("event-spec-v3.yaml"));
        for (Invalid result : results) {
            LOGGER.severe(result.toString());
        }
    }

    private Path file(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(name).getFile()).toPath();
    }
}
