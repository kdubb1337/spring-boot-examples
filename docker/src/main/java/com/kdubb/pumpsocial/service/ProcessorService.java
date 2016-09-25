package com.kdubb.pumpsocial.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.processor.AbstractImporter;
import com.kdubb.pumpsocial.processor.FacebookImporter;
import com.kdubb.pumpsocial.processor.GooglePlusImporter;
import com.kdubb.pumpsocial.processor.InstagramImporter;
import com.kdubb.pumpsocial.processor.TumblrImporter;
import com.kdubb.pumpsocial.processor.TwitterImporter;

@Service
public class ProcessorService {
	
//	@Inject
//	private RssImporter rssImporter;
	
	@Inject
	private InstagramImporter instagramImporter;
	
	@Inject
	private GooglePlusImporter googlePlusImporter;
	
	@Inject
	private TwitterImporter twitterImporter;
	
	@Inject
	private FacebookImporter facebookImporter;
	
	@Inject
	private TumblrImporter tumblrImporter;
	
	private Map<SocialConnectionType, AbstractImporter> importerMap;
	
	@PostConstruct
	public void init() {
		importerMap = new HashMap<SocialConnectionType, AbstractImporter>();
//		importerMap.put(SocialConnectionType.rss, rssImporter);
		importerMap.put(SocialConnectionType.instagram, instagramImporter);
		importerMap.put(SocialConnectionType.google, googlePlusImporter);
		importerMap.put(SocialConnectionType.twitter, twitterImporter);
		importerMap.put(SocialConnectionType.facebook, facebookImporter);
		importerMap.put(SocialConnectionType.tumblr, tumblrImporter);
	}
	
	public AbstractImporter getImporter(SocialConnectionType type) {
		return importerMap.get(type);
	}
}