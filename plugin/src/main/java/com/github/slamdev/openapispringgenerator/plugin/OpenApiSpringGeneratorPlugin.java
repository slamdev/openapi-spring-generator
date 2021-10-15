package com.github.slamdev.openapispringgenerator.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;

public class OpenApiSpringGeneratorPlugin implements Plugin<Project> {

    private static final String PATH = "openapi-generated-sources";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        OpenApiTask task = project.getTasks().create("openapi", OpenApiTask.class, t -> t.setDestinationDir(calculateDestination(project)));
        project.getTasks().withType(JavaCompile.class, t -> t.dependsOn(task));
        project.getTasks().withType(ProcessResources.class, t -> t.dependsOn(task));
        JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet sourceSet = extension.getSourceSets().getByName("main");
        sourceSet.java(dir -> dir.srcDir(task.getDestinationDir().toString() + "/main/java"));
        sourceSet.resources(dir -> dir.srcDir(task.getDestinationDir().toString() + "/main/resources"));
    }

    private File calculateDestination(Project project) {
        return project.file(project.getBuildDir() + "/" + PATH);
    }
}
