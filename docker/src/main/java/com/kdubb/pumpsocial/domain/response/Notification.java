package com.kdubb.pumpsocial.domain.response;

public class Notification {

	private String title;
	private Object body;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}
}
