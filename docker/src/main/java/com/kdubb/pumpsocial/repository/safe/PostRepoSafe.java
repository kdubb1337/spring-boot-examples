package com.kdubb.pumpsocial.repository.safe;

import java.util.List;

import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.repository.PostRepository;

@Service
public class PostRepoSafe extends AbstractRepeaterRepo<Post, String> implements PostRepository {

	@Inject
	private PostRepository postRepo;

	@Override
	protected MongoRepository<Post, String> getRepo() {
		return postRepo;
	}

	@Override
	public List<Post> findByLinkLike(final String link) {
		return repeaterService.retryIfNecessary(() -> postRepo.findByLinkLike(link));
	}

	@Override
	public Long countByUserId(final String userId) {
		return repeaterService.retryIfNecessary(() -> postRepo.countByUserId(userId));
	}

	@Override
	public Long deleteByUserId(String userId) {
		return repeaterService.retryIfNecessary(() -> postRepo.deleteByUserId(userId));
	}

	@Override
	public Page<Post> findByUserIdOrderByScrapeTimeDesc(String userId, Pageable page) {
		return repeaterService.retryIfNecessary(() -> postRepo.findByUserIdOrderByScrapeTimeDesc(userId, page));
	}
}