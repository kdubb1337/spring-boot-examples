package com.kdubb.pumpsocial.service;

import java.util.Calendar;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.CrawledPage;
import com.kdubb.pumpsocial.repository.safe.CrawledPageRepoSafe;

@Service
public class CrawledPageService {

	@Inject
	private CrawledPageRepoSafe crawledPageRepo;
	
	public boolean isPageCrawled(String link) {
		CrawledPage existing = crawledPageRepo.findByLink(link);
		return existing != null;
	}
	
	public void setPageCrawled(String link) {
		setPageCrawled(link, false);
	}
	
	public void setPageCrawled(String link, boolean isError) {
		// Is already set as crawled?
		if(isPageCrawled(link)) {
			return;
		}
		
		CrawledPage page = new CrawledPage();
		page.setLink(link);
		page.setErrored(isError);
		page.setTimestamp(Calendar.getInstance().getTime());
		crawledPageRepo.save(page);
	}
}