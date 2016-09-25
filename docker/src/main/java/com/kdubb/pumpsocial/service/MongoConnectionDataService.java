package com.kdubb.pumpsocial.service;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.mongodb.MongoConnection;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.FacebookProfile;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.repository.safe.MongoConnectionRepoSafe;

@Service
public class MongoConnectionDataService {

	@Inject
	private MongoConnectionRepoSafe mongoConnectionDataRepo;

	@Inject
	private SocialConnectionService socialConnectionService;
	
	@Inject
	private ConnectionRepository connectionRepository;
	
	private static final Logger LOG = LogManager.getLogger(MongoConnectionDataService.class);

	public void deleteByUserId(String userId) {
		List<MongoConnection> connections = mongoConnectionDataRepo.findByUserId(userId);
		connections.forEach(x -> socialConnectionService.deleteByMongoConnection(x));
		mongoConnectionDataRepo.delete(connections);
	}

	public void deleteByUserIdAndProviderId(String userId, String providerId) {
		if(StringUtils.isBlank(userId) || StringUtils.isBlank(providerId))
			return;
		
		List<MongoConnection> connections = mongoConnectionDataRepo.findByUserIdAndProviderId(userId, providerId);
		connections.forEach(x -> socialConnectionService.deleteByMongoConnection(x));
		mongoConnectionDataRepo.delete(connections);

		List<SocialConnection> socialConnections = socialConnectionService.findByUserId(userId);
		socialConnections.stream()
			.filter(conn -> conn.getType() == null || providerId.equals(conn.getType().toString()))
			.forEach(conn -> socialConnectionService.delete(conn));
	}
	
	public List<MongoConnection> findByUserId(String userId) {
		return mongoConnectionDataRepo.findByUserId(userId);
	}

	public List<MongoConnection> findByUserIdAndProviderId(String userId, String providerId) {
		return mongoConnectionDataRepo.findByUserIdAndProviderId(userId, providerId);
	}
	
	public MongoConnection findByProviderIdAndProviderUserId(String providerId, String providerUserId) {
		return mongoConnectionDataRepo.findByProviderIdAndProviderUserId(providerId, providerUserId);
	}
	
	public MongoConnection findByUserIdAndProviderIdAndProviderUserId(String userId, String providerId, String providerUserId) {
		return mongoConnectionDataRepo.findByUserIdAndProviderIdAndProviderUserId(userId, providerId, providerUserId);
	}
	
	public String getConnectionName(MongoConnection connection) {
		switch(connection.getProviderId()) {
			case "facebook":
				Facebook facebook = connectionRepository.findPrimaryConnection(Facebook.class).getApi();
				FacebookProfile profile = facebook.userOperations().getUserProfile();
				LOG.info("name=" + profile.getLastName() + ", " + profile.getFirstName());
				return profile.getLastName() + ", " + profile.getFirstName();
			case "rss":
				return connection.getProfileUrl();
			default:
				return "unknown type";
		}
	}

	public void save(MongoConnection conn) {
		mongoConnectionDataRepo.save(conn);
	}
}