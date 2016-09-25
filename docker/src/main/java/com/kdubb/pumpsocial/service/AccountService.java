package com.kdubb.pumpsocial.service;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Account;
import com.kdubb.pumpsocial.repository.safe.AccountRepoSafe;

@Service
public class AccountService {

	@Inject
	private AccountRepoSafe accountRepo;

	@Inject
	private PostService postService;
	
	@Inject
	private MongoConnectionDataService mongoConnectionDataService;
	
	private static final Logger LOG = LogManager.getLogger(AccountService.class);
	
	public void save(Account account) {
		accountRepo.save(account);
	}
	
	public Account findByUsername(String username) {
		return accountRepo.findByUsername(username);
	}
	
	public Account findByEmail(String email) {
		return accountRepo.findByEmail(email);
	}

	public void deleteAccount(String userId) {
		Account account = findByUsername(userId);
		LOG.info("Deleting account for email '" + account.getEmail() + "'");
		
		// Delete posts
		postService.deleteByUserId(userId);
		
		// Delete connections, related socialConnections and related routes
		mongoConnectionDataService.deleteByUserId(userId);
		
		// Delete account
		accountRepo.delete(account);
	}
}
