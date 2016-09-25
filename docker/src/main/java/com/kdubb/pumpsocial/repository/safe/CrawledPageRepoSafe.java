package com.kdubb.pumpsocial.repository.safe;

import javax.inject.Inject;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.CrawledPage;
import com.kdubb.pumpsocial.repository.CrawledPageRepository;

@Service
public class CrawledPageRepoSafe extends AbstractRepeaterRepo<CrawledPage, String> implements CrawledPageRepository {

	@Inject
	private CrawledPageRepository pageRepo;

	@Override
	protected MongoRepository<CrawledPage, String> getRepo() {
		return pageRepo;
	}

	@Override
	public CrawledPage findByLink(final String link) {
		return repeaterService.retryIfNecessary(() -> pageRepo.findByLink(link));
	}
}
