package com.github.slamdev.openapispringgenerator.generator;

import org.junit.Test;

import java.nio.file.Paths;

public class GeneratorTest {

    @Test
    public void should_generate() {
        Generator g = new Generator();
        g.generate(
                Paths.get("showcase/server/src/main/resources/openapi/petstore-expanded.yaml"),
                Generator.Type.SERVER,
                Paths.get("build/output")
        );
    }
}
