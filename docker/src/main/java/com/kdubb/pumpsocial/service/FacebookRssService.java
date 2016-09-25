package com.kdubb.pumpsocial.service;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.api.client.FacebookRssClient;
import com.kdubb.pumpsocial.domain.facebook.FacebookWall;

@Service
public class FacebookRssService {

	@Inject
	private FacebookRssClient facebookRssClient;
	
	private static final Logger LOG = LogManager.getLogger(FacebookRssService.class);
	
	public FacebookWall getWall(String type, String id) {
		return facebookRssClient.getWall(type, id);
	}
}
