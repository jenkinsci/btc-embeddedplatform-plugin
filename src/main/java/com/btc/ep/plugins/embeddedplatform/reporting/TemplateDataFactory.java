package com.btc.ep.plugins.embeddedplatform.reporting;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * This class implemented the template data factory.
 * 
 * @author AlNe
 */
public class TemplateDataFactory {

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
}
