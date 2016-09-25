package com.kdubb.pumpsocial;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SpringBootWebSecurityConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration(exclude = { ThymeleafAutoConfiguration.class, SecurityAutoConfiguration.class, SpringBootWebSecurityConfiguration.class,
		FallbackWebSecurityAutoConfiguration.class, DataSourceAutoConfiguration.class })
@ComponentScan(basePackages = { "com.kdubb.pumpsocial", "org.springframework.social.connect.mongodb" })
public class Application {

	@Value("${myvar}")
	private String var;

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	@PostConstruct
	public void init() {
		LOG.info("var initialized to '{}'", var);
	}

	@RequestMapping("/hello")
	public String home() {
		return "Hello Docker World";
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}