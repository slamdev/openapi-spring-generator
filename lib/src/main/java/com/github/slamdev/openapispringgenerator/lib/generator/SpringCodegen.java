package com.github.slamdev.openapispringgenerator.lib.generator;

import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpringCodegen extends AbstractJavaCodegen implements OptionalFeatures {

    public enum Type {
        SERVER, CLIENT, CONSUMER, PRODUCER
    }

    private boolean useOptional;

    public SpringCodegen() {
        super();
        projectFolder = "";
        sourceFolder = "";
        supportedLibraries.put(Type.SERVER.name().toLowerCase(Locale.ROOT), "RestController interface");
        supportedLibraries.put(Type.CLIENT.name().toLowerCase(Locale.ROOT), "RestTemplate client");
        supportedLibraries.put(Type.CONSUMER.name().toLowerCase(Locale.ROOT), "StreamListener interface");
        supportedLibraries.put(Type.PRODUCER.name().toLowerCase(Locale.ROOT), "Stream client");
        additionalProperties.put(DATE_LIBRARY, "java8");
        additionalProperties.put(JAVA8_MODE, true);
    }

    @Override
    protected void setTemplateEngine() {
        templateEngine = new FormattedTemplateEngine(this);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        apiTemplateFiles.put("api.mustache", ".java");
        if (Arrays.asList("client", "producer").contains(getLibrary())) {
            supportingFiles.add(new SupportingFile("org.springframework.boot.autoconfigure.AutoConfiguration.imports.mustache", "META-INF/spring", "org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        }
        modelTemplateFiles.put("model.mustache", ".java");
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");
        apiTestTemplateFiles.remove("api_test.mustache");
        importMapping.remove("Schema");
        typeMapping.put("file", "Resource");
        typeMapping.put("binary", "Resource");
        importMapping.put("Resource", "org.springframework.core.io.Resource");
        typeMapping.put("DateTime", "Instant");
        importMapping.put("Instant", "java.time.Instant");
        if (additionalProperties.containsKey(OptionalFeatures.USE_OPTIONAL)) {
            setUseOptional(convertPropertyToBoolean(OptionalFeatures.USE_OPTIONAL));
            writePropertyBack(USE_OPTIONAL, useOptional);
        }
    }

    private boolean isStream() {
        return Arrays.asList("consumer", "producer").contains(getLibrary());
    }

    @Override
    public String toApiName(String name) {
        String suffix = "Api";
        if ("consumer".equals(getLibrary())) {
            suffix = "StreamConsumer";
        } else if ("producer".equals(getLibrary())) {
            suffix = "StreamProducer";
        }
        if (vendorExtensions.containsKey("x-api-name-prefix")) {
            name = vendorExtensions.get("x-api-name-prefix").toString();
            return name + suffix;
        }
        if (name.isEmpty()) {
            return "Default" + suffix;
        }
        return camelize(name) + suffix;
    }

    @Override
    public String modelPackage() {
        if (vendorExtensions.containsKey("x-package-name")) {
            return vendorExtensions.get("x-package-name").toString();
        }
        return modelPackage;
    }

    @Override
    public String apiPackage() {
        if (vendorExtensions.containsKey("x-package-name")) {
            return vendorExtensions.get("x-package-name").toString();
        }
        return apiPackage;
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return sanitizeName(camelize(property.name));
    }

    @Override
    public String getDefaultTemplateDir() {
        return getName();
    }

    @Override
    public CodegenType getTag() {
        if (Arrays.asList("consumer", "client").contains(getLibrary())) {
            return CodegenType.CLIENT;
        }
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName().toLowerCase(Locale.getDefault());
    }

    @Override
    public String getHelp() {
        return "Generates code for Spring app.";
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
        CodegenModel model = super.fromModel(name, schema, allSchemas);
        model.imports.remove("Schema");
        return model;
    }

    @Override
    protected void postProcessAllCodegenModels(Map<String, CodegenModel> allModels) {
        super.postProcessAllCodegenModels(allModels);
        allModels.forEach((k, model) -> {
            if (model.getParent() == null && (model.getChildren() == null || model.getChildren().isEmpty())) {
                model.getVendorExtensions().put("x-inheritance", false);
            } else {
                model.getVendorExtensions().put("x-inheritance", true);
            }
        });
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        if (openAPI.getPaths().isEmpty()) {
            supportingFiles.removeIf(f -> "org.springframework.boot.autoconfigure.AutoConfiguration.imports.mustache".equals(f.templateFile));
        }
        if (!isStream()) {
            return;
        }
        for (PathItem path : openAPI.getPaths().values()) {
            if (path.readOperations().size() > 1) {
                throw new IllegalStateException("Only one operation per path is allowed for streams generation");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        objs = super.postProcessOperations(objs);
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation operation : ops) {
                if (vendorExtensions.containsKey("x-security-role")) {
                    operation.vendorExtensions.putIfAbsent("x-security-role", vendorExtensions.get("x-security-role"));
                }
                if (operation.returnType == null || "Void".equals(operation.returnType)) {
                    operation.vendorExtensions.putIfAbsent("x-void", true);
                }
                if (vendorExtensions.containsKey("x-path-variable-name")) {
                    operation.vendorExtensions.putIfAbsent("x-path-variable-name", vendorExtensions.get("x-path-variable-name"));
                }
                if (vendorExtensions.containsKey("x-entity")) {
                    operation.vendorExtensions.putIfAbsent("x-entity", vendorExtensions.get("x-entity"));
                }
                List<CodegenResponse> responses = operation.responses;
                if (responses != null) {
                    for (final CodegenResponse resp : responses) {
                        doDataTypeAssignment(resp.dataType, new DataTypeAssigner() {
                            @Override
                            public void setReturnType(final String returnType) {
                                resp.dataType = returnType;
                            }

                            @Override
                            public void setReturnContainer(final String returnContainer) {
                                resp.containerType = returnContainer;
                            }
                        });
                        if ("0".equals(resp.code)) {
                            resp.code = "200";
                        }
                        int code = 200;
                        try {
                            code = Integer.parseInt(resp.code);
                        } catch (NumberFormatException ignored) {
                        }
                        operation.vendorExtensions.putIfAbsent("x-response-code", code);
                    }
                }

                doDataTypeAssignment(operation.returnType, new DataTypeAssigner() {

                    @Override
                    public void setReturnType(final String returnType) {
                        operation.returnType = returnType;
                    }

                    @Override
                    public void setReturnContainer(final String returnContainer) {
                        operation.returnContainer = returnContainer;
                    }
                });

                if (isStream()) {
                    String topic = operation.path.replaceFirst("/", "");
                    operation.vendorExtensions.putIfAbsent("x-topic-name", topic);
                    String camelCased = camelizeVarName(topic, false);
                    operation.vendorExtensions.putIfAbsent("x-topic-class", camelCased);
                    operation.vendorExtensions.putIfAbsent("x-topic-const", underscore(camelCased + "_topic").toUpperCase(Locale.getDefault()));
                }
            }
        }

        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        if (imports != null && apiPackage().equals(modelPackage())) {
            imports.removeIf(p -> {
                String pkg = p.get("import");
                return pkg.startsWith(apiPackage());
            });
        }
        return objs;
    }

    @Override
    protected String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        if (isStream() && StringUtils.isBlank(operation.getOperationId())) {
            String topic = camelizeVarName(path, false);
            if (CodegenType.CLIENT.equals(getTag())) {
                return "process" + topic;
            }
            return "send" + topic;
        }
        return super.getOrGenerateOperationId(operation, path, httpMethod);
    }

    private void doDataTypeAssignment(String returnType, DataTypeAssigner dataTypeAssigner) {
        if (returnType == null) {
            dataTypeAssigner.setReturnType("Void");
        } else if (returnType.startsWith("List")) {
            int end = returnType.lastIndexOf('>');
            if (end > 0) {
                dataTypeAssigner.setReturnType(returnType.substring("List<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("List");
            }
        } else if (returnType.startsWith("Map")) {
            int end = returnType.lastIndexOf('>');
            if (end > 0) {
                String mapTypes = returnType.substring("Map<".length(), end);
                String mapKey = mapTypes.split(",")[0];
                String mapValue = mapTypes.substring(mapKey.length() + 1).trim();
                dataTypeAssigner.setReturnType(mapValue);
                dataTypeAssigner.setReturnContainer("Map");
            }
        } else if (returnType.startsWith("Set")) {
            int end = returnType.lastIndexOf('>');
            if (end > 0) {
                dataTypeAssigner.setReturnType(returnType.substring("Set<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("Set");
            }
        }
    }

    @Override
    public void setUseOptional(boolean useOptional) {
        this.useOptional = useOptional;
    }

    private interface DataTypeAssigner {
        void setReturnType(String returnType);

        void setReturnContainer(String returnContainer);
    }

    @Override
    protected void addProducesInfo(ApiResponse response, CodegenOperation codegenOperation) {
        super.addProducesInfo(response, codegenOperation);
        if (isStream() || response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return;
        }
        boolean wildcard = response.getContent().keySet().stream().anyMatch("*/*"::equals);
        if (wildcard) {
            String message = String.format(
                    "'%s (%s)' operation doesn't specify produces media type or specify it as wildcard, spring does not support that",
                    codegenOperation.path, codegenOperation.operationId
            );
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    protected void addConsumesInfo(Operation operation, CodegenOperation codegenOperation, OpenAPI openAPI) {
        super.addConsumesInfo(operation, codegenOperation, openAPI);
        if (isStream() || operation.getRequestBody() == null) {
            return;
        }
        boolean wildcard = operation.getRequestBody().getContent().keySet().stream().anyMatch("*/*"::equals);
        if (wildcard) {
            String message = String.format(
                    "'%s (%s)' operation doesn't specify consumes media type or specify it as wildcard, spring does not support that",
                    codegenOperation.path, codegenOperation.operationId
            );
            throw new IllegalArgumentException(message);
        }
    }
}
