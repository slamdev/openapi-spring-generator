package com.github.slamdev.openapispringgenerator.cli;

import com.github.slamdev.openapispringgenerator.lib.validator.Validator;
import org.openapitools.codegen.validation.Invalid;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "validate", mixinStandardHelpOptions = true, description = "validate spec file")
public class Validate implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"}, required = true, description = "path to a spec file")
    private Path specFile;

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void run() {
        List<Invalid> results = new Validator().validate(specFile);
        for (Invalid result : results) {
            System.err.printf("%s: %s%n", result.getRule().getSeverity(), result.getMessage());
        }
    }
}
