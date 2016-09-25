package com.kdubb.pumpsocial.service;

import java.io.IOException;
import java.time.Instant;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Account;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class ProblemService {

	@Inject
	private EmailService emailService;
	
	@Inject
	private AccountService accountService;
	
	@Inject
	private SocialConnectionService socialConnectionService;

	private static final Logger LOG = LogManager.getLogger(ProblemService.class);
	
	public void authorizationProblem(SocialConnection source) {
		Account account = accountService.findByUsername(source.getUserId());
		
		if(account == null) {
			LOG.error("Couldn't find account for userId=[" + source.getUserId() + "]");
			return;
		}
		
		try {
			emailService.sendAuthExceptionEmail(account.getEmail(), source.getType().toString(), socialConnectionService.getSocialConnectionName(source));
		} catch (IOException e) {
			LOG.error("Failed to send AuthExceptionEmail", e);
		}
		
		source.setIsActive(false);
		source.setLastEmailMills(Instant.now().toEpochMilli());
		
		if(source.getEmailCount() == null) {
			source.setEmailCount(1);
		}
		else {
			source.setEmailCount(source.getEmailCount() + 1);
		}
		
		LOG.info("Saving --> " + Utils.toPrettyJson(source));
		socialConnectionService.update(source, source.getUserId());
	}
}