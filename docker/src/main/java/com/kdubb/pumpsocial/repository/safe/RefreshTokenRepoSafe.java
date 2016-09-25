package com.kdubb.pumpsocial.repository.safe;

import javax.inject.Inject;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.RefreshToken;
import com.kdubb.pumpsocial.repository.RefreshTokenRepository;

@Service
public class RefreshTokenRepoSafe extends AbstractRepeaterRepo<RefreshToken, String> implements RefreshTokenRepository {

	@Inject
	private RefreshTokenRepository refreshTokenRepo;

	@Override
	protected MongoRepository<RefreshToken, String> getRepo() {
		return refreshTokenRepo;
	}

	@Override
	public RefreshToken findByProviderIdAndProviderUserId(String providerId, String providerUserId) {
		return repeaterService.retryIfNecessary(() -> refreshTokenRepo.findByProviderIdAndProviderUserId(providerId, providerUserId));
	}
}