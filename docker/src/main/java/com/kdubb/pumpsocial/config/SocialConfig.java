package com.kdubb.pumpsocial.config;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.mongodb.ConnectionConverter;
import org.springframework.social.connect.mongodb.ConnectionService;
import org.springframework.social.connect.mongodb.MongoConnectionService;
import org.springframework.social.connect.mongodb.MongoUsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.ReconnectFilter;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.facebook.web.DisconnectController;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.social.instagram.connect.InstagramConnectionFactory;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.linkedin.connect.LinkedInConnectionFactory;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.social.tumblr.connect.TumblrConnectionFactory;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.connect.TwitterConnectionFactory;

import com.kdubb.pumpsocial.ConfigConstants;
import com.kdubb.pumpsocial.service.WebsocketService;

@Configuration
@EnableSocial
public class SocialConfig implements SocialConfigurer {

	@Value(ConfigConstants.FB_KEY)
	private String fbKey;

	@Value(ConfigConstants.FB_SECRET)
	private String fbSecret;

	@Value(ConfigConstants.TWITTER_KEY)
	private String twitterKey;

	@Value(ConfigConstants.TWITTER_SECRET)
	private String twitterSecret;

	@Value(ConfigConstants.LINKEDIN_KEY)
	private String linkedInKey;

	@Value(ConfigConstants.LINKEDIN_SECRET)
	private String linkedInSecret;

	@Value(ConfigConstants.TUMBLR_KEY)
	private String tumblrKey;

	@Value(ConfigConstants.TUMBLR_SECRET)
	private String tumblrSecret;

	@Value(ConfigConstants.GOOGLE_KEY)
	private String googleKey;

	@Value(ConfigConstants.GOOGLE_SECRET)
	private String googleSecret;

	@Value(ConfigConstants.INSTAGRAM_KEY)
	private String instagramKey;

	@Value(ConfigConstants.INSTAGRAM_SECRET)
	private String instagramSecret;

	@Inject
	private MongoTemplate template;

	@Inject
	private ConnectionConverter connectionConverter;

	@Inject
	private WebsocketService websocketService;

	// @Autowired
	// private ConnectionFactoryLocator connectionFactoryLocator;
	//
	// @Autowired
	// private UsersConnectionRepository connectionRepository;

	private static final Logger LOG = LogManager.getLogger(SocialConfig.class);

	@Override
	public void addConnectionFactories(ConnectionFactoryConfigurer cfConfig, Environment env) {
		cfConfig.addConnectionFactory(new FacebookConnectionFactory(fbKey, fbSecret));
		cfConfig.addConnectionFactory(new TwitterConnectionFactory(twitterKey, twitterSecret));
		cfConfig.addConnectionFactory(new LinkedInConnectionFactory(linkedInKey, linkedInSecret));
		cfConfig.addConnectionFactory(new TumblrConnectionFactory(tumblrKey, tumblrSecret));
		cfConfig.addConnectionFactory(new GoogleConnectionFactory(googleKey, googleSecret));
		cfConfig.addConnectionFactory(new InstagramConnectionFactory(instagramKey, instagramSecret));

		LOG.info("instagramKey=" + instagramKey);
		LOG.info("instagramSecret=" + instagramSecret);
	}

	@Override
	public UserIdSource getUserIdSource() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication == null)
				throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in");

			return authentication.getName();
		};
	}

	// @Bean
	// public ProviderSignInUtils providerSignInUtils() {
	// return new ProviderSignInUtils(connectionFactoryLocator, connectionRepository);
	// }

	@Bean
	public ConnectionService connectionService() {
		return new MongoConnectionService(template, connectionConverter);
	}

	@Bean
	@Override
	public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
		MongoUsersConnectionRepository repository = new MongoUsersConnectionRepository(websocketService, connectionService(), connectionFactoryLocator,
				Encryptors.noOpText());
		return repository;
	}

	// API Binding Beans

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public Facebook facebook(ConnectionRepository repository) {
		Connection<Facebook> connection = repository.findPrimaryConnection(Facebook.class);
		return connection != null ? connection.getApi() : null;
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public Twitter twitter(ConnectionRepository repository) {
		Connection<Twitter> connection = repository.findPrimaryConnection(Twitter.class);
		return connection != null ? connection.getApi() : null;
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public LinkedIn linkedin(ConnectionRepository repository) {
		Connection<LinkedIn> connection = repository.findPrimaryConnection(LinkedIn.class);
		return connection != null ? connection.getApi() : null;
	}

	@Bean
	@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
	public Tumblr tumblr(ConnectionRepository repository) {
		Connection<Tumblr> connection = repository.findPrimaryConnection(Tumblr.class);
		return connection != null ? connection.getApi() : null;
	}

	// Web Controller and Filter Beans

	@Bean
	public ConnectController connectController(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository) {
		ConnectController connectController = new ConnectController(connectionFactoryLocator, connectionRepository);
		// connectController.addInterceptor(new PostToWallAfterConnectInterceptor());
		// connectController.addInterceptor(new TweetAfterConnectInterceptor());
		return connectController;
	}

	@Bean
	public ProviderSignInController providerSignInController(ConnectionFactoryLocator connectionFactoryLocator,
			UsersConnectionRepository usersConnectionRepository, SignInAdapter signinAdapter) {
		return new ProviderSignInController(connectionFactoryLocator, usersConnectionRepository, signinAdapter);
	}

	@Bean
	public DisconnectController disconnectController(UsersConnectionRepository usersConnectionRepository, Environment env) {
		return new DisconnectController(usersConnectionRepository, fbSecret);
	}

	@Bean
	public ReconnectFilter apiExceptionHandler(UsersConnectionRepository usersConnectionRepository, UserIdSource userIdSource) {
		return new ReconnectFilter(usersConnectionRepository, userIdSource);
	}
}