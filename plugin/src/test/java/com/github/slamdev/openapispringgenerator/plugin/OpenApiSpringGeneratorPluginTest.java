package com.github.slamdev.openapispringgenerator.plugin;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class OpenApiSpringGeneratorPluginTest {

    private OpenApiTask task;

    @Before
    public void setUp() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(OpenApiSpringGeneratorPlugin.class);
        task = (OpenApiTask) project.getTasks().getByName("openapi");
    }

    @Test
    public void should_run_task() throws IOException {
        task.consumer(file("event-spec-v3.yaml"));
        task.producer(file("event-spec-v3.yaml"));
        task.setDestinationDir(new File("build/output"));
        task.run();
    }

    private File file(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }
}
