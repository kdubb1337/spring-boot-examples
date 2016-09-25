package com.kdubb.pumpsocial.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.social.connect.mongodb.MongoConnection;

public interface MongoConnectionDataRepository extends MongoRepository<MongoConnection, String>{

	public List<MongoConnection> findByUserId(String userId);
	
	public Page<MongoConnection> findByUserId(String userId, Pageable pageable);
	
	public List<MongoConnection> findByUserIdAndProviderId(String userId, String providerId);

	public MongoConnection findByProviderIdAndProviderUserId(String providerId, String providerUserId);
	
	public MongoConnection findByUserIdAndProviderIdAndProviderUserId(String userId, String providerId, String providerUserId);
}