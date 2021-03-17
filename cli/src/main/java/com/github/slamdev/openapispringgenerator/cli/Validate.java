package com.github.slamdev.openapispringgenerator.cli;

import com.github.slamdev.openapispringgenerator.lib.validator.Validator;
import org.openapitools.codegen.validation.Invalid;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "validate", mixinStandardHelpOptions = true, description = "validate spec file")
public class Validate implements Runnable {

    @CommandLine.Parameters(arity = "1..*", description = "path to a spec file")
    private List<Path> specFiles;

    @CommandLine.Option(names = {"--fail-on-errors"}, description = "fail cli if errors found")
    private boolean failOnErrors;

    @Override
    @SuppressWarnings({"PMD.SystemPrintln", "PMD.DoNotTerminateVM"})
    public void run() {
        boolean failed = false;
        for (Path specFile : specFiles) {
            List<Invalid> results = new Validator().validate(specFile);
            if (results.isEmpty()) {
                continue;
            }
            failed = true;
            System.err.printf("errors found in %s%n", specFile.toString());
            for (Invalid result : results) {
                System.err.printf("%s: %s%n", result.getRule().getSeverity(), result.getMessage());
            }
        }
        if (failOnErrors && failed) {
            System.exit(1);
        }
    }
}
