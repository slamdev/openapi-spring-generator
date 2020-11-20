package com.github.slamdev.openapispringgenerator.plugin.validator;

import org.junit.Test;
import org.openapitools.codegen.validation.Invalid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ValidatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatorTest.class);

    @Test
    public void should_validate() {
        Validator v = new Validator();
        List<Invalid> results = v.validate(file("event-spec-v3.yaml"));
        for (Invalid result : results) {
            LOGGER.error("{}", result);
        }
    }

    private Path file(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(name).getFile()).toPath();
    }
}