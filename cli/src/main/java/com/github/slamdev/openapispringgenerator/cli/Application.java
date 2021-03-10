package com.github.slamdev.openapispringgenerator.cli;

import picocli.CommandLine;

@CommandLine.Command(subcommands = {Generate.class, Validate.class})
public class Application {

    public static void main(String[] args) {
        int exitCode = new CommandLine(Application.class).execute(args);
        System.exit(exitCode);
    }
}
