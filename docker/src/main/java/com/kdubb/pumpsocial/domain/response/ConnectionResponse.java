package com.kdubb.pumpsocial.domain.response;

import java.util.List;

public class ConnectionResponse {

	private String id;
	private String name;
	private String displayName;
	private String profileUrl;
	private String imageUrl;
	private boolean isSource = false;
	private boolean isTarget = false;
	
	private List<ConnectionResponse> pages;
	
	public boolean isSource() {
		return isSource;
	}

	public void setSource(boolean isSource) {
		this.isSource = isSource;
	}

	public boolean isTarget() {
		return isTarget;
	}

	public void setTarget(boolean isTarget) {
		this.isTarget = isTarget;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ConnectionResponse> getPages() {
		return pages;
	}

	public void setPages(List<ConnectionResponse> pages) {
		this.pages = pages;
	}

	public String getProfileUrl() {
		return profileUrl;
	}
	
	public void setProfileUrl(String profileUrl) {
		this.profileUrl = profileUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}