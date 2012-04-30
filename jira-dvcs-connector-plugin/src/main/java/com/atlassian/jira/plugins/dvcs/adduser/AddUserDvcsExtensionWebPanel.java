package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.templaterenderer.TemplateRenderer;

public class AddUserDvcsExtensionWebPanel implements WebPanel {

	private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

	private final TemplateRenderer templateRenderer;

	public AddUserDvcsExtensionWebPanel(TemplateRenderer templateRenderer) {
		this.templateRenderer = templateRenderer;
	}

	@Override
	public String getHtml(Map<String, Object> model) {
		
		StringWriter stringWriter = new StringWriter();
	
		try {
			
			templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);
			
		} catch (Exception e) {
			log.warn("Error while rendering DVCS extension fragment for add user form.", e);
			stringWriter = new StringWriter(); // reset writer so no broken output goes out
		} 
		
		return stringWriter.toString();
	}

	@Override
	public void writeHtml(Writer writer, Map<String, Object> model)
			throws IOException {
		System.out.println("===");
	}

}
