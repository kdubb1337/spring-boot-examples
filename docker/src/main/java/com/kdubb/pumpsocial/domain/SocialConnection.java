package com.kdubb.pumpsocial.domain;

import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.social.connect.mongodb.MongoConnection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kdubb.pumpsocial.Constants;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;

@Document
public class SocialConnection extends MongoBase {

	@Indexed
	private String userId;

	private SocialConnectionType type;
	private String typeId;
	private String url;
	private String imageUrl;
	private String name;
	private String pageName;
	private Boolean isSource;
	private Boolean isTarget;
	private Boolean isPrimary;
	private String pumpTag;

	private boolean isActive;
	private Integer emailCount;
	private Long lastEmailMills;

	private List<String> permissions;

	@DBRef(lazy = true)
	@JsonIgnore
	private MongoConnection connection;

	@DBRef(lazy = true)
	@JsonIgnore
	private SocialConnection parent;

	@Transient
	private List<SocialConnection> pages;

	public String getPumpTag() {
		if (StringUtils.isBlank(pumpTag))
			return Constants.DEFAULT_TRIGGER_TAG;

		return pumpTag;
	}

	public void setPumpTag(String pumpTag) {
		this.pumpTag = pumpTag;
	}

	public boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Integer getEmailCount() {
		return emailCount;
	}

	public void setEmailCount(Integer emailCount) {
		this.emailCount = emailCount;
	}

	public Long getLastEmailMills() {
		return lastEmailMills;
	}

	public void setLastEmailMills(Long lastEmailMills) {
		this.lastEmailMills = lastEmailMills;
	}

	public List<String> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<String> permissions) {
		this.permissions = permissions;
	}

	public Boolean getIsPrimary() {
		return isPrimary;
	}

	public void setIsPrimary(Boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	public List<SocialConnection> getPages() {
		return pages;
	}

	public void setPages(List<SocialConnection> pages) {
		this.pages = pages;
	}

	public SocialConnection getParent() {
		return parent;
	}

	public void setParent(SocialConnection parent) {
		this.parent = parent;
	}

	public MongoConnection getConnection() {
		return connection;
	}

	public void setConnection(MongoConnection connection) {
		this.connection = connection;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Boolean getIsSource() {
		return isSource;
	}

	public void setIsSource(Boolean isSource) {
		this.isSource = isSource;
	}

	public Boolean getIsTarget() {
		return isTarget;
	}

	public void setIsTarget(Boolean isTarget) {
		this.isTarget = isTarget;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public SocialConnectionType getType() {
		return type;
	}

	public void setType(SocialConnectionType type) {
		this.type = type;
	}

	public String getTypeId() {
		return typeId;
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	public void merge(SocialConnection existing) {
		if (StringUtils.isNotBlank(userId))
			existing.setUserId(userId);

		if (type != null)
			existing.setType(type);

		if (StringUtils.isNotBlank(typeId))
			existing.setTypeId(typeId);

		if (StringUtils.isNotBlank(url))
			existing.setUrl(url);

		if (StringUtils.isNotBlank(imageUrl))
			existing.setImageUrl(imageUrl);

		if (StringUtils.isNotBlank(name))
			existing.setName(name);

		if (StringUtils.isNotBlank(pageName))
			existing.setPageName(pageName);

		if (StringUtils.isNotBlank(pumpTag))
			existing.setPumpTag(pumpTag);

		if (isSource != null)
			existing.setIsSource(isSource);

		if (isTarget != null)
			existing.setIsTarget(isTarget);

		if (isPrimary != null)
			existing.setIsPrimary(isPrimary);

		existing.setIsActive(isActive);

		if (emailCount != null)
			existing.setEmailCount(emailCount);

		if (lastEmailMills != null)
			existing.setLastEmailMills(lastEmailMills);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(userId).append(type).append(typeId).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}

		SocialConnection rhs = (SocialConnection) obj;
		return new EqualsBuilder().append(userId, rhs.userId).append(type, rhs.type).append(typeId, rhs.typeId).isEquals();
	}
}