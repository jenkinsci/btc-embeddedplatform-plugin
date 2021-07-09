package com.btc.ep.plugins.embeddedplatform.reporting;

import java.nio.file.Path;
import java.util.Map;

/**
 * The implementation of the template data.
 * 
 * @author AlNe
 */
public class TemplateData {

    private Path template;
    private String relativePath;
    private Map<String, Object> data;

    /**
     * The constructor.
     *
     * @param template The template path.
     * @param data The template data model.
     * @param relativePath The template relative path.
     */
    public TemplateData(Path template, Map<String, Object> data, String relativePath) {
        this.template = template;
        this.relativePath = relativePath;
        this.data = data;
    }

    /**
     * Set the template path.
     *
     * @param template The template path to be set.
     */
    public void setTemplatePath(Path template) {
        this.template = template;
    }

    public Path getTemplatePath() {
        return template;
    }

    /**
     * Set the template data model.
     *
     * @param data The template data model to be set.
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Set the relative path for the resulted HTML.
     *
     * @param relativePath The relative path to be set.
     */
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getRelativePath() {
        return relativePath;
    }
}
