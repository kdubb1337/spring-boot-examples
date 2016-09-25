package com.kdubb.pumpsocial.repository.safe;

import java.util.List;

import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.social.connect.mongodb.MongoConnection;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.repository.MongoConnectionDataRepository;

@Service
public class MongoConnectionRepoSafe extends AbstractRepeaterRepo<MongoConnection, String> implements MongoConnectionDataRepository {

	@Inject
	private MongoConnectionDataRepository connectionRepo;

	@Override
	protected MongoRepository<MongoConnection, String> getRepo() {
		return connectionRepo;
	}

	@Override
	public List<MongoConnection> findByUserIdAndProviderId(final String userId, final String providerId) {
		return repeaterService.retryIfNecessary(() -> connectionRepo.findByUserIdAndProviderId(userId, providerId));
	}

	@Override
	public MongoConnection findByUserIdAndProviderIdAndProviderUserId(final String userId, final String providerId, final String providerUserId) {
		return repeaterService.retryIfNecessary(() -> connectionRepo.findByUserIdAndProviderIdAndProviderUserId(userId, providerId, providerUserId));
	}

	@Override
	public List<MongoConnection> findByUserId(final String userId) {
		return repeaterService.retryIfNecessary(() -> connectionRepo.findByUserId(userId));
	}

	@Override
	public Page<MongoConnection> findByUserId(final String userId, final Pageable pageable) {
		return repeaterService.retryIfNecessary(() -> connectionRepo.findByUserId(userId, pageable));
	}

	@Override
	public MongoConnection findByProviderIdAndProviderUserId(final String providerId, final String providerUserId) {
		return repeaterService.retryIfNecessary(() -> connectionRepo.findByProviderIdAndProviderUserId(providerId, providerUserId));
	}
}