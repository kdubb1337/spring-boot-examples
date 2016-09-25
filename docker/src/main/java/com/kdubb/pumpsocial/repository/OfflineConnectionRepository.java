package com.kdubb.pumpsocial.repository;

import javax.inject.Inject;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.google.api.Google;
import org.springframework.social.instagram.api.Instagram;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;

@Service
public class OfflineConnectionRepository {
	
	@Inject
	private UsersConnectionRepository usersConnectionRepository;
	
	public <T> Connection<T> getConnection(String userId, Class<T> clazz) {
		ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(userId);
		
		if(connectionRepository == null)
			return null;
		
		return connectionRepository.getPrimaryConnection(clazz);
	}
	
	public <T> T getConnectionApi(String userId, Class<T> clazz) {
		Connection<T> connection = getConnection(userId, clazz);
		
		if(connection == null)
			return null;
		
		return connection.getApi();
	}
	
	public Connection<?> getConnectionApi(String userId, String name) {
		switch(name) {
			case "facebook":
				return getConnection(userId, Facebook.class);
			case "google":
				return getConnection(userId, Google.class);
			case "linkedin":
				return getConnection(userId, LinkedIn.class);
			case "tumblr":
				return getConnection(userId, Tumblr.class);
			case "twitter":
				return getConnection(userId, Twitter.class);
			case "instagram":
				return getConnection(userId, Instagram.class);
		}
		
		return null;
	}
}