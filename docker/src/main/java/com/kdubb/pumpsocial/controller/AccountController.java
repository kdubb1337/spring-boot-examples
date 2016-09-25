package com.kdubb.pumpsocial.controller;

import java.security.Principal;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.kdubb.pumpsocial.domain.Account;
import com.kdubb.pumpsocial.service.AccountService;
import com.kdubb.pumpsocial.service.PostService;
import com.kdubb.pumpsocial.util.Utils;

@RestController
@RequestMapping("/account")
public class AccountController {
	
	@Inject
	private AccountService accountService;
	
	@Inject
	private PostService postService;
	
	private static final Logger LOG = LogManager.getLogger(AccountController.class);
	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public Account getAccount(Principal currentUser) {
		Account account = accountService.findByUsername(currentUser.getName());
		
		long numPosts = postService.getPostCount(currentUser.getName());
		account.setNumPosts(numPosts);
		
		//LOG.info("account=" + Utils.toPrettyJson(account));
		
		return account;
	}
	
	@RequestMapping(value = "", method = RequestMethod.POST)
	public Account updateAccount(@RequestBody Account account, Principal currentUser) {
		Account existingAccount = accountService.findByUsername(currentUser.getName());
		existingAccount.merge(account);
		accountService.save(existingAccount);
		return existingAccount;
	}
	
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	public Boolean deleteAccount(Principal currentUser, HttpServletRequest request) {
		accountService.deleteAccount(currentUser.getName());
		request.getSession().invalidate();
		return true;
	}
}
