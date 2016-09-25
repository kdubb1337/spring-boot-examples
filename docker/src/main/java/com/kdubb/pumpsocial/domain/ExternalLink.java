package com.kdubb.pumpsocial.domain;

public class ExternalLink {

	public static enum LinkType {
		HTML, VIDEO
	}
	
	private LinkType type;
	private String link;
	
	public LinkType getType() {
		return type;
	}
	
	public void setType(LinkType type) {
		this.type = type;
	}
	
	public String getLink() {
		return link;
	}
	
	public void setLink(String link) {
		this.link = link;
	}
}