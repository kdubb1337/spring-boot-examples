package com.kdubb.pumpsocial.domain.facebook;

import java.util.Date;

public class FacebookWallEntry {

	private String title;
	private String id;
	private String alternate;
	private String[] categories;
	private Date published;
	private Date updated;
	private FacebookWallAuthor author;
	private String verb;
	private String target;
	private String objects;
	private String comments;
	private String likes;
	private String content;
	
	public String getAlternate() {
		return alternate;
	}
	
	public void setAlternate(String alternate) {
		this.alternate = alternate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String[] getCategories() {
		return categories;
	}

	public void setCategories(String[] categories) {
		this.categories = categories;
	}

	public Date getPublished() {
		return published;
	}

	public void setPublished(Date published) {
		this.published = published;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public FacebookWallAuthor getAuthor() {
		return author;
	}

	public void setAuthor(FacebookWallAuthor author) {
		this.author = author;
	}

	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getObjects() {
		return objects;
	}

	public void setObjects(String objects) {
		this.objects = objects;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getLikes() {
		return likes;
	}

	public void setLikes(String likes) {
		this.likes = likes;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
