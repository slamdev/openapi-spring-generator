package com.github.slamdev.openapispringgenerator.plugin.generator;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.templates.HandlebarTemplateEngine;

import java.io.IOException;
import java.util.Map;

public class FormattedTemplateEngine extends HandlebarTemplateEngine {

    private final Formatter formatter = new Formatter();

    public FormattedTemplateEngine(CodegenConfig config) {
        super(config);
    }

    @Override
    public String getRendered(String templateFile, Map<String, Object> templateData) throws IOException {
        String content = super.getRendered(templateFile, templateData);
        try {
            return formatter.formatSource(content);
        } catch (FormatterException e) {
            throw new IOException(e);
        }
    }
}
