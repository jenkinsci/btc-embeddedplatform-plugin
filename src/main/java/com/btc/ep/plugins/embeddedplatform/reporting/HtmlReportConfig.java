package com.btc.ep.plugins.embeddedplatform.reporting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HTML Report configurator; the implementation */
public class HtmlReportConfig {

    private Path resourceDirectory;
    private Path mainTemplate;
    private Map<String, Object> mainModel;
    private List<TemplateData> additionalTemplates;
    HtmlReportCreator _creator;
    private String type;
    private String name;
    private Map<String, String> metaData;
    private boolean createCalled;

    /**
     * Constructor.
     *
     * @param type the typ of report (this is not "HTML", but rather "Model coverage report".)
     * @param creator the helper that creates the actual report
     * @param templateFactory The template data factory.
     */
    public HtmlReportConfig(
        String type,
        HtmlReportCreator creator) {

        checkArgument(creator != null);
        checkArgument(type != null);

        this._creator = creator;
        this.type = type;
        this.additionalTemplates = new ArrayList<>();
        this.metaData = new HashMap<>();
    }

    public File create() {
        // Sanity-check and avoid recursive invocations from extensions that try to be funny.
        try {
            sanityCheck();
            createCalled = true;
            File report = _creator.createHTMLReport(this);
            checkState(report != null);
            return report;

        } finally {
            createCalled = false;
        }
    }

    public void createWithoutStoring(File targetDir) {
        try {
            sanityCheck();
            createCalled = true;
            _creator.createHTMLReportWithoutStoringReport(this, targetDir);

        } finally {
            createCalled = false;
        }
    }

    /** Sanity check my state before going into report generation. */
    private void sanityCheck() {
        checkState(!createCalled);
        checkState(mainTemplate != null);
        checkState(mainModel != null);
        checkState(name != null);
    }

    public HtmlReportConfig mainTemplate(Path mainTemplate) {
        checkArgument(mainTemplate != null);
        checkArgument(mainTemplate.toFile() != null);
        checkArgument(mainTemplate.toFile().exists());
        checkArgument(!mainTemplate.toFile().isDirectory());

        this.mainTemplate = mainTemplate;
        return this;
    }

    public HtmlReportConfig mainModel(Map<String, Object> mainModel) {
        checkArgument(mainModel != null);

        this.mainModel = mainModel;
        return this;
    }

    public HtmlReportConfig resourceDirectory(Path resourceDirectory) {
        checkArgument(resourceDirectory != null);
        checkArgument(resourceDirectory.toFile() != null);
        checkArgument(resourceDirectory.toFile().exists());
        checkArgument(resourceDirectory.toFile().isDirectory());

        this.resourceDirectory = resourceDirectory;
        return this;
    }

    public HtmlReportConfig addTemplateWithOwnData(Path template, Map<String, Object> data, String relativePath) {
        checkArgument(template != null);
        checkArgument(template.toFile() != null);
        checkArgument(template.toFile().exists());
        checkArgument(!template.toFile().isDirectory());
        checkArgument(data != null);
        return this;
    }

    public HtmlReportConfig name(String name) {
        checkArgument(name != null);
        checkArgument(!name.isEmpty());

        this.name = name;
        return this;
    }

    public HtmlReportConfig addMetaData(String key, String value) {
        checkArgument(key != null);
        checkArgument(!key.isEmpty());
        checkArgument(value != null);

        this.metaData.put(key, value);
        return this;
    }

    public HtmlReportConfig addTemplateForMainData(Path template) {
        checkArgument(template != null);
        checkArgument(template.toFile() != null);
        checkArgument(template.toFile().exists());
        checkArgument(!template.toFile().isDirectory());

        TemplateData templateData = createTemplateData(template);
        getAdditionalTemplates().add(templateData);
        return this;
    }

    public String getType() {
        return type;
    }

    public Path getResourceDirectory() {
        return resourceDirectory;
    }

    public Path getMainFileTemplate() {
        return mainTemplate;
    }

    public Map<String, Object> getMainFileModel() {
        return mainModel;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public String getName() {
        return name;
    }

    public TemplateData createTemplateData(Path template, Map<String, Object> data, String relativePath) {
        checkArgument(template != null, "Template path must not be null!");

        String relPath = relativePath;
        if (relPath == null || relPath.isEmpty()) {
            File templateFile = template.toFile();
            relPath = templateFile.getName().replace(".ftl", ".html").replace(".ftlh", ".html");
        }

        return new TemplateData(template, data, relPath);
    }

    public TemplateData createTemplateData(Path template) {
        return createTemplateData(template, null, null);
    }

    public List<TemplateData> getAdditionalTemplates() {
        return additionalTemplates;
        
    }
}
