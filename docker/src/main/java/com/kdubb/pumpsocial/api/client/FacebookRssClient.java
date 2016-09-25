package com.kdubb.pumpsocial.api.client;

import com.kdubb.pumpsocial.api.FacebookRssApi;
import com.kdubb.pumpsocial.domain.facebook.FacebookWall;

public class FacebookRssClient {

	private FacebookRssApi api;
	
	public FacebookRssClient(FacebookRssApi api) {
		this.api = api;
	}
	
	public FacebookWall getWall(String type, String id) {
		return api.getWall(type, id);
	}
}