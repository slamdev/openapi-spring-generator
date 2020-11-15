package com.github.slamdev.openapispringgenerator.plugin.generator;

import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpringCodegen extends AbstractJavaCodegen implements OptionalFeatures {

    private boolean useOptional;

    public SpringCodegen() {
        super();
        projectFolder = "";
        sourceFolder = "";
        supportedLibraries.put("server", "");
        supportedLibraries.put("client", "");
        additionalProperties.put(DATE_LIBRARY, "java8");
        additionalProperties.put(JAVA8_MODE, true);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        apiTemplateFiles.put("api.mustache", ".java");
        if (CodegenType.CLIENT.equals(getTag())) {
            supportingFiles.add(new SupportingFile("spring.factories.mustache", "META-INF", "spring.factories"));
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

    @Override
    public String toApiName(String name) {
        if (vendorExtensions.containsKey("x-api-name-prefix")) {
            return vendorExtensions.get("x-api-name-prefix").toString() + "Api";
        }
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return camelize(name) + "Api";
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
        return CodegenType.forValue(library);
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
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        model.imports.remove("Schema");
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
}
