package com.btc.ep.plugins.embeddedplatform.http;

import java.util.List;
import java.util.Map;

import org.apache.http.StatusLine;

import com.google.gson.Gson;

public class GenericResponse {

	private String content;
	private StatusLine status;

	/**
	 * Get content.
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;

	}

	/**
	 * Set content.
	 * 
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;

	}

	/**
	 * Get status.
	 * 
	 * @return the status
	 */
	public StatusLine getStatus() {
		return status;

	}

	/**
	 * Set status.
	 * 
	 * @param status the status to set
	 */
	public void setStatus(StatusLine status) {
		this.status = status;

	}

	public Map<String, Object> getContentAsMap() {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = new Gson().fromJson(content, Map.class);
		return map;
	}

	public List<Object> getContentAsList() {
		@SuppressWarnings("unchecked")
		List<Object> list = new Gson().fromJson(content, List.class);
		return list;
	}

}
