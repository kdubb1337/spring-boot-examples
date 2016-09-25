package com.kdubb.pumpsocial.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.domain.CrawledPage;

public interface CrawledPageRepository extends MongoRepository<CrawledPage, String> {

	public CrawledPage findByLink(String link);
}
