package com.kdubb.pumpsocial.repository.safe;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.social.connect.mongodb.MongoConnection;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.repository.SocialConnectionRepository;

@Service
public class SocialConnectionRepoSafe extends AbstractRepeaterRepo<SocialConnection, ObjectId> implements SocialConnectionRepository {

	@Inject
	private SocialConnectionRepository socialConnectionRepo;

	@Override
	protected MongoRepository<SocialConnection, ObjectId> getRepo() {
		return socialConnectionRepo;
	}

	@Override
	public List<SocialConnection> findByUserId(final String userId) {
		return repeaterService.retryIfNecessary(() -> socialConnectionRepo.findByUserId(userId));
	}

	@Override
	public Page<SocialConnection> findByUserId(final String userId, final Pageable pageable) {
		return repeaterService.retryIfNecessary(() -> socialConnectionRepo.findByUserId(userId, pageable));
	}

	@Override
	public Set<SocialConnection> findByConnection(final MongoConnection connection) {
		return repeaterService.retryIfNecessary(() -> socialConnectionRepo.findByConnection(connection));
	}

	@Override
	public Set<SocialConnection> findByUserIdAndParentIsNull(final String userId) {
		return repeaterService.retryIfNecessary(() -> socialConnectionRepo.findByUserIdAndParentIsNull(userId));
	}
}