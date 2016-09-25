package com.kdubb.pumpsocial.service;

import javax.inject.Inject;
import javax.inject.Provider;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.google.api.Google;
import org.springframework.social.instagram.api.Instagram;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class ApiService {

	@Inject
	private Provider<ConnectionRepository> connectionRepositoryProvider;
	
	@Inject
	private RepeaterService repeaterService;
	
	public Facebook getFacebook() {
		return findPrimaryConnection(Facebook.class).getApi();
	}
	
	public Twitter getTwitter() {
		return findPrimaryConnection(Twitter.class).getApi();
	}
	
	public Tumblr getTumblr() {
		return findPrimaryConnection(Tumblr.class).getApi();
	}
	
	public Google getGoogle() {
		return findPrimaryConnection(Google.class).getApi();
	}
	
	public Instagram getInstagram() {
		return findPrimaryConnection(Instagram.class).getApi();
	}
	
	public MultiValueMap<String, Connection<?>> findAllConnections() {
		return repeaterService.retryIfNecessary(() -> connectionRepositoryProvider.get().findAllConnections());
	}
	
	private <T> Connection<T> findPrimaryConnection(final Class<T> t) {
		return repeaterService.retryIfNecessary(() -> connectionRepositoryProvider.get().findPrimaryConnection(t));
	}
}