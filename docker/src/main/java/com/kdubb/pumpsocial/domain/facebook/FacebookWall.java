package com.kdubb.pumpsocial.domain.facebook;

import java.util.List;

public class FacebookWall {

	private String title;
	private String link;
	private String self;
	private String updated;
	private String icon;
	private List<FacebookWallEntry> entries;
	
	public List<FacebookWallEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<FacebookWallEntry> entries) {
		this.entries = entries;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}