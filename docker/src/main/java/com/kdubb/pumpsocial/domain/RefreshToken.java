package com.kdubb.pumpsocial.domain;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class RefreshToken extends MongoBase {

	private String providerId;
	private String providerUserId;
	private String refreshToken;
	
	public String getProviderId() {
		return providerId;
	}
	
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	
	public String getProviderUserId() {
		return providerUserId;
	}
	
	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}