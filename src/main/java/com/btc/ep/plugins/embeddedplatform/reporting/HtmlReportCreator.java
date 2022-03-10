package com.btc.ep.plugins.embeddedplatform.reporting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

import com.btc.ep.plugins.embeddedplatform.util.Util;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/** HTML report creator. */
public class HtmlReportCreator {

	static final String HTML_REPORT = "report.html";
	static final String REL_PATH_TEMPLATE = "reporting/templates/base_report_original.ftl";
	static final String REL_PATH_CSS = "reporting/templates/styles.css";
	static final String REL_PATH_JS = "reporting/templates/scripts.js";
	static final String BASE_REPORT_FILE = "base_report.ftl";
	static final String CSS_FILE = "styles.css";
	static final String JS_FILE = "scripts.js";

	protected File _workDir;

	/**
	 * <p>
	 * Create the HTML report.
	 * </p>
	 * <p>
	 * Creates a model element for the report, with a managed directory. Creates a
	 * working directory and copies all templates to that directory before calling
	 * possible extension call-backs; it is thus safe for all extensions to mess
	 * around with all the templats in the workind directory. The HTML is then
	 * generated via freemarker library, and persisted in the report DMOs managed
	 * directory.
	 *
	 * @param config the config
	 * @return the report
	 * @throws IOException
	 * @throws IllegalArgumentException if the config is null
	 */
	protected File createHTMLReport(HtmlReportConfig config) {
		checkArgument(config != null);

		File renderDir;
		try {
			renderDir = Files.createTempDirectory(null).toFile();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		_copyToWorkDir(config);
		_writeMetaInfo(config);

		File report = _createHTMLReport(renderDir.toPath(), config);
		return report;
	}

	protected void createHTMLReportWithoutStoringReport(HtmlReportConfig config, File targetDir) {
		checkArgument(config != null);

		_copyToWorkDir(config);
		_writeMetaInfo(config);

		_createHTMLReport(targetDir.toPath(), config);
		_copyResourceFolder(targetDir.toPath(), config);
	}

	/**
	 * Copy the whole given tmo directory to the given report folder.
	 *
	 * @param tmpFolder    to copy
	 * @param reportFolder target folder
	 */
	protected void _doCopy(File tmpFolder, File reportFolder) {
		try {
			FileUtils.copyDirectory(tmpFolder, reportFolder);

		} catch (IOException e) {
			throw new IllegalStateException("Cannot copy finished report to managed directory.");
		}
	}

	/**
	 * Add data for the general meta data section of each report. User name who
	 * created the report Date when this report was created
	 *
	 * @param config report config
	 */
	protected void _writeMetaInfo(HtmlReportConfig config) {
		Map<String, Object> data = config.getMainFileModel();
		data.put("creator", System.getProperty("user.name"));
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
		data.put("date", dateFormat.format(new Date()));
		data.put("os", new StringBuilder().append(System.getProperty("os.name")).append(" ")
				.append(System.getProperty("os.arch")).append(" ").append(System.getProperty("os.version")).toString());
	}

	/**
	 * Copy all templates to the working directory.
	 *
	 * @param config the config
	 */
	public void _copyToWorkDir(HtmlReportConfig config) {
		if (_workDir == null) {
			try {
				_workDir = Files.createTempDirectory("workDir").toFile();

				File baseTemplate = Util.getResourceAsFile(getClass(), REL_PATH_TEMPLATE);
				File baseTemplateStyle = Util.getResourceAsFile(getClass(), REL_PATH_CSS);
				File baseTemplateScript = Util.getResourceAsFile(getClass(), REL_PATH_JS);

				checkState(baseTemplate != null);
				checkState(baseTemplate.exists());
				checkState(!baseTemplate.isDirectory());
				checkState(baseTemplateStyle != null);
				checkState(baseTemplateStyle.exists());
				checkState(!baseTemplateStyle.isDirectory());
				checkState(baseTemplateScript != null);
				checkState(baseTemplateScript.exists());
				checkState(!baseTemplateScript.isDirectory());

				copyBaseTemplates(baseTemplate, baseTemplateStyle, baseTemplateScript);

			} catch (IOException e) {
				throw new IllegalStateException("Cannot copy the base templates to work dir.", e);
			}
		}
		try {
			List<TemplateData> aditionnalReportTemplates = config.getAdditionalTemplates();
			for (TemplateData templateData : aditionnalReportTemplates) {
				Path additionalFilePath = templateData.getTemplatePath();
				File additionalFile = additionalFilePath.toFile();
				if (!_workDir.equals(additionalFile.getParentFile())) {
					FileUtils.copyFileToDirectory(additionalFile, _workDir);
					Path workDirTemplatePath = Paths.get(_workDir.getAbsolutePath(), additionalFile.getName());
					templateData.setTemplatePath(workDirTemplatePath);
				}
			}

			File mainFile = config.getMainFileTemplate().toFile();
			if (!_workDir.equals(mainFile.getParentFile())) {
				FileUtils.copyFileToDirectory(mainFile, _workDir);
				config.mainTemplate(Paths.get(_workDir.getAbsolutePath(), mainFile.getName()));
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot copy templates to work dir.", e);
		}
	}

	/**
	 * Method that generates the HTML files from the template files
	 *
	 * @param reportDir the location of the report folder
	 * @param config    the variables which will be injected in the HTML
	 */
	public File _createHTMLReport(Path reportDir, HtmlReportConfig config) {
		FileWriter fileWriter = null;
		try {
			Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setLogTemplateExceptions(false);
			cfg.setDirectoryForTemplateLoading(_workDir);
			cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);

			// Process main template
			File mainFile = config.getMainFileTemplate().toFile();
			Template main = cfg.getTemplate(mainFile.getName());

			File f = new File(reportDir + "/" + config.getName() + ".html");
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(f.getAbsolutePath()), StandardCharsets.UTF_8));
			main.process(config.getMainFileModel(), out);
			out.close();
			return f;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Copies the resource folder from resource path to report dir path
	 *
	 * @param reportDir the path where to copy the resource directory
	 * @param config    the path from where to copy the resource directory
	 */
	public void _copyResourceFolder(Path reportDir, HtmlReportConfig config) {
		try {
			File destinationFolder = reportDir.toFile();
			if (config.getResourceDirectory() != null) {
				File resourceFolder = config.getResourceDirectory().toFile();
				if (resourceFolder.exists()) {
					FileUtils.copyDirectoryToDirectory(resourceFolder, destinationFolder);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Copies the base template files (base_report, style and script).<br>
	 * If it's a multi-file report, add external links and copy the style and script
	 * to 2 different files.<br>
	 * If it's NOT a multi-file report, copy the CSS and JS into the base template.
	 * 
	 * 
	 *
	 * @param baseTemplate       - base template file
	 * @param baseTemplateStyle  - style file
	 * @param baseTemplateScript - script file
	 * @throws IOException IO Exception
	 */
	private void copyBaseTemplates(File baseTemplate, File baseTemplateStyle, File baseTemplateScript)
			throws IOException {
		String replacedCSS = "";
		String replacedCSSAndJSVersion = "";

		String wholeBaseFTL = getStringFromFile(baseTemplate);
		String wholeCSS = getStringFromFile(baseTemplateStyle);
		String wholeJS = getStringFromFile(baseTemplateScript);

		File newBaseTemplateFile = new File(baseTemplate.getParentFile().getAbsolutePath() + "\\" + BASE_REPORT_FILE);

		checkState(newBaseTemplateFile != null);

		replacedCSS += wholeBaseFTL.replaceAll("#addStyleHere",
				Matcher.quoteReplacement("<style>" + wholeCSS + "</style>"));

		replacedCSSAndJSVersion += replacedCSS.replaceAll("#addScriptHere",
				Matcher.quoteReplacement("<script>" + wholeJS + "</script>"));

		try {
			OutputStream outputStream = new FileOutputStream(newBaseTemplateFile);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			outputStreamWriter.write(replacedCSSAndJSVersion);
			outputStreamWriter.close();

			FileUtils.copyFileToDirectory(newBaseTemplateFile, _workDir);
			newBaseTemplateFile.delete();

		} catch (Exception e) {
			throw new IOException("Cannot copy the base template files");
		}
	}

	/**
	 * Return into a string the whole containing text from given template. Throws
	 * IOException.
	 * 
	 * @param baseTemplate - base template file
	 *
	 * @return a string containing the whole file
	 * @throws IOException IO exception
	 */
	private String getStringFromFile(File baseTemplate) throws IOException {
		return new String(Files.readAllBytes(baseTemplate.toPath()));
	}

}
