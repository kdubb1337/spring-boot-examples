package com.kdubb.pumpsocial.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.domain.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

	public RefreshToken findByProviderIdAndProviderUserId(String providerId, String providerUserId);
}
