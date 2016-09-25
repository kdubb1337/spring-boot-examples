package com.kdubb.pumpsocial.service;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.connect.Connection;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.google.api.Google;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.util.Utils;

@Service
public class UserInfoService {

	private static final Logger LOG = LogManager.getLogger(UserInfoService.class);
	
	public String getEmail(Connection<?> connection) {
		if(connection == null)
			return null;
		
		Object api = connection.getApi();
		
		if(api == null)
			return null;
		
		LOG.info("api=" + api.getClass());
		
		if(Facebook.class.isAssignableFrom(api.getClass())) {
			Facebook facebook = (Facebook)api;
			LOG.info("facebook=" + Utils.toPrettyJson(facebook.userOperations().getUserProfile()));
			return facebook.userOperations().getUserProfile().getEmail();
		}
		
		if(Google.class.isAssignableFrom(api.getClass())) {
			Google google = (Google)api;
			LOG.info("google=" + google.userOperations().getUserInfo().getEmail());
			return google.userOperations().getUserInfo().getEmail();
		}
		
		return null;
	}
	
	public List<String> getPermissions(Connection<?> connection) {
		if(connection == null)
			return null;
		
		Object api = connection.getApi();
		
		if(api == null)
			return null;

		if(Facebook.class.isAssignableFrom(api.getClass())) {
			Facebook facebook = (Facebook)api;
			return facebook.userOperations().getUserPermissions();
		}
		else if(Google.class.isAssignableFrom(api.getClass())) {
//			Google google = (Google)api;
			LOG.warn("Google privilages unknown");
		}
		else if(Twitter.class.isAssignableFrom(api.getClass())) {
//			Twitter twitter = (Twitter)api;
			LOG.warn("Twitter privilages unknown");
		}
		
		return null;
	}
}
