package com.github.slamdev.openapispringgenerator.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

public class OpenApiSpringGeneratorPlugin implements Plugin<Project> {

    private static final String PATH = "openapi-generated-sources";

    @Override
    public void apply(Project project) {
        if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
            throw new IllegalStateException("Project should have JavaPlugin to be applied first");
        }
        OpenApiTask task = project.getTasks().create("openapi", OpenApiTask.class);
        task.setDestinationDir(project.file(project.getBuildDir().toString() + "/" + PATH));
        project.getTasks().stream()
                .filter(JavaCompile.class::isInstance)
                .forEach(t -> t.dependsOn(task));

        JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet sourceSet = convention.getSourceSets().getByName("main");
        sourceSet.java(dir -> dir.srcDir(task.getDestinationDir().toString() + "/main/java"));
        sourceSet.resources(dir -> dir.srcDir(task.getDestinationDir().toString() + "/main/resources"));
    }
}
