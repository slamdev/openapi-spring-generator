package com.github.slamdev.openapispringgenerator.plugin.validator;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class ValidatorTest {

    @Test
    public void should_validate() {
        Validator v = new Validator();
        v.validate(file("spec.yaml"));
    }

    private Path file(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(name).getFile()).toPath();
    }
}