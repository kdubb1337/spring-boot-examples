package com.kdubb.pumpsocial.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.domain.Account;

public interface AccountRepository extends MongoRepository<Account, String> {
	
	public Account findByUsername(String username);

	public Account findByEmail(String email);
}