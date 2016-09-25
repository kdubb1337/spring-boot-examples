package com.kdubb.pumpsocial.repository.safe;

import javax.inject.Inject;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Account;
import com.kdubb.pumpsocial.repository.AccountRepository;

@Service
public class AccountRepoSafe extends AbstractRepeaterRepo<Account, String> implements AccountRepository {

	@Inject
	private AccountRepository accountRepo;

	@Override
	protected MongoRepository<Account, String> getRepo() {
		return accountRepo;
	}

	@Override
	public Account findByUsername(final String username) {
		return repeaterService.retryIfNecessary(() -> accountRepo.findByUsername(username));
	}

	@Override
	public Account findByEmail(final String email) {
		return repeaterService.retryIfNecessary(() -> accountRepo.findByEmail(email));
	}
}