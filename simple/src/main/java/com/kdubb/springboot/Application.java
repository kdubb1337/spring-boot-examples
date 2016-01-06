package com.kdubb.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;

@ComponentScan
@Configuration
@EnableAutoConfiguration
public class Application {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		init();
		SpringApplication.run(Application.class, args);
	}

	private static void init() {
		String defaultEnv = System.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
		String env = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, defaultEnv);

		LOG.warn("Starting with environment -> {" + env + "}");
	}
}