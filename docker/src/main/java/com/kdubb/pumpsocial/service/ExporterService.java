package com.kdubb.pumpsocial.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.exporter.Exporter;
import com.kdubb.pumpsocial.exporter.FacebookExporter;
import com.kdubb.pumpsocial.exporter.GoogleExporter;
import com.kdubb.pumpsocial.exporter.TumblrExporter;
import com.kdubb.pumpsocial.exporter.TwitterExporter;

@Service
public class ExporterService {

	@Inject
	private FacebookExporter facebookExporter;

	@Inject
	private TwitterExporter twitterExporter;
	
	@Inject
	private GoogleExporter googleExporter;
	
	@Inject
	private TumblrExporter tumblrExporter;
	
	private Map<SocialConnectionType, Exporter> exporterMap;
	
	@PostConstruct
	public void init() {
		exporterMap = new HashMap<SocialConnectionType, Exporter>();
		exporterMap.put(SocialConnectionType.facebook, facebookExporter);
		exporterMap.put(SocialConnectionType.twitter, twitterExporter);
		exporterMap.put(SocialConnectionType.google, googleExporter);
		exporterMap.put(SocialConnectionType.tumblr, tumblrExporter);
	}
	
	public Exporter getExporter(SocialConnectionType type) {
		return exporterMap.get(type);
	}
}