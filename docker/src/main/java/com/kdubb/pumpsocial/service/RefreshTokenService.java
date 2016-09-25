package com.kdubb.pumpsocial.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.RefreshToken;
import com.kdubb.pumpsocial.repository.safe.RefreshTokenRepoSafe;

@Service
public class RefreshTokenService {

	@Inject
	private RefreshTokenRepoSafe refreshTokenRepo;
	
	public void save(RefreshToken token) {
		refreshTokenRepo.save(token);
	}
	
	public void delete(String id) {
		refreshTokenRepo.delete(id);
	}
	
	public RefreshToken findOne(String id) {
		return refreshTokenRepo.findOne(id);
	}
	
	public RefreshToken findByProviderIdAndProviderUserId(String providerId, String providerUserId) {
		return refreshTokenRepo.findByProviderIdAndProviderUserId(providerId, providerUserId);
	}
}