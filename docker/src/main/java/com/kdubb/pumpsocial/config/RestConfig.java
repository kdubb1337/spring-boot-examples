package com.kdubb.pumpsocial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kdubb.pumpsocial.api.FacebookRssApi;
import com.kdubb.pumpsocial.api.client.FacebookRssClient;
import com.kdubb.pumpsocial.factory.RestFactory;

@Configuration
public class RestConfig {

	@Bean
	public FacebookRssClient facebookRssClient() {
		RestFactory factory = new RestFactory("https://www.facebook.com/");
		FacebookRssApi api = factory.create(FacebookRssApi.class);
		return new FacebookRssClient(api);
	}
}