package com.github.slamdev.openapispringgenerator.generator;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GeneratorTest {

    @Test
    public void should_generate() {
        Generator g = new Generator();
        g.generate(file("spec.yaml"), Generator.Type.CLIENT, Paths.get("build/output"), true);
    }

    private Path file(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(name).getFile()).toPath();
    }
}
