package com.kdubb.pumpsocial.domain;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.enums.PostType;
import com.kdubb.pumpsocial.util.Utils;

@Document
public class Post extends MongoBase {

	private String userId;
	private String title;
	private String content;
	
	@Indexed
	private String link; // ie. URL of the post (or if unavailable, a unique ID to avoid double posting)
	private String imageUrl;
	private String videoUrl;
	
	private boolean uploadImage;
	private Boolean isIgnore;
	
	private long publishTime;
	private long scrapeTime;
	
	private PostType type;
	
	private List<String> tags;
	private List<ExternalLink> externalLinks;
	
	private Collection<SocialConnection> targets;
	
	@DBRef
	private SocialConnection source;

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public PostType getType() {
		return type;
	}

	public void setType(PostType type) {
		this.type = type;
	}

	public List<ExternalLink> getExternalLinks() {
		return externalLinks;
	}

	public void setExternalLinks(List<ExternalLink> externalLinks) {
		this.externalLinks = externalLinks;
	}

	public Boolean getIsIgnore() {
		return isIgnore;
	}

	public void setIsIgnore(Boolean isIgnore) {
		this.isIgnore = isIgnore;
	}

	public Collection<SocialConnection> getTargets() {
		return targets;
	}

	public void setTargets(Collection<SocialConnection> targets) {
		this.targets = targets;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public long getScrapeTime() {
		return scrapeTime;
	}

	public void setScrapeTime(long scrapeTime) {
		this.scrapeTime = scrapeTime;
	}

	public SocialConnection getSource() {
		return source;
	}

	public void setSource(SocialConnection source) {
		this.source = source;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long getPublishTime() {
		return publishTime;
	}

	public void setPublishTime(long publishTime) {
		this.publishTime = publishTime;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public boolean isUploadImage() {
		return uploadImage;
	}

	public void setUploadImage(boolean uploadImage) {
		this.uploadImage = uploadImage;
	}
	
	public void removeTag(String tag) {
		if(StringUtils.isNotBlank(getContent())) {
			String content = Utils.removeTag(getContent(), tag);
			
			if(content != null)
				content = content.trim();
			
			setContent(content);
		}

		if(!CollectionUtils.isEmpty(getTags()))
			getTags().remove(tag);
	}
}