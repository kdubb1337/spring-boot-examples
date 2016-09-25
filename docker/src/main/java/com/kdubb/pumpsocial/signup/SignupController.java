/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kdubb.pumpsocial.signup;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.web.ProviderSignInUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;

import com.kdubb.pumpsocial.domain.Account;
import com.kdubb.pumpsocial.service.AccountService;
import com.kdubb.pumpsocial.service.EmailService;
import com.kdubb.pumpsocial.service.SocialConnectionService;
import com.kdubb.pumpsocial.service.UserInfoService;
import com.kdubb.pumpsocial.signin.SignInUtils;
import com.kdubb.pumpsocial.util.Utils;

@Controller
public class SignupController {

	@Inject
	private AccountService accountService;

	@Inject
	private UserInfoService userInfoService;

	@Inject
	private SocialConnectionService connectionService;

	@Inject
	private EmailService emailService;

	private final ProviderSignInUtils providerSignInUtils = new ProviderSignInUtils();

	private static final Logger LOG = LogManager.getLogger(SignupController.class);

	private Account fromUserProfile(Connection<?> connection, String email) {
		UserProfile providerUser = connection.fetchUserProfile();

		Account account = new Account();
		account.setFirstName(providerUser.getFirstName());
		account.setLastName(providerUser.getLastName());
		account.setUsername(UUID.randomUUID().toString());
		account.setEmail(email);

		return account;
	}

	@RequestMapping(value = "/signup", method = RequestMethod.GET)
	public String signupForm(WebRequest request) {
		Connection<?> connection = providerSignInUtils.getConnectionFromSession(request);

		if (connection != null) {
			String email = userInfoService.getEmail(connection);
			Account account = accountService.findByEmail(email);

			if (account == null) {
				account = fromUserProfile(connection, email);
				accountService.save(account);

				SignInUtils.signin(account.getUsername());

				providerSignInUtils.doPostSignUp(account.getUsername(), request);

				connectionService.findByUserId(account.getUsername()).stream().filter(x -> x.getParent() == null) // Is
																													// it
																													// the
																													// root
																													// connection?
						.forEach(conn -> {
							LOG.info("connnnnn --> " + Utils.toPrettyJson(conn));
							conn.setIsPrimary(true);
							connectionService.save(conn);
						});

				try {
					emailService.sendWelcomeEmail(account.getEmail(), account.getFirstName());
				}
				catch (IOException e) {
					LOG.error("Failed to sendWelcomeEmail", e);
				}
			} else {
				SignInUtils.signin(account.getUsername());
				providerSignInUtils.doPostSignUp(account.getUsername(), request);
			}

			LOG.info("account=" + Utils.toPrettyJson(account));
		}

		return "redirect:/";
	}

	// @RequestMapping(value="/signup", method=RequestMethod.POST)
	// public String signup(@Valid SignupForm form, BindingResult formBinding, WebRequest request) {
	// if (formBinding.hasErrors())
	// return null;
	//
	// Account account = createAccount(form);
	//
	// LOG.info("account=" + account);
	//
	// if (account == null)
	// return null;
	//
	// SignInUtils.signin(account.getUsername());
	// providerSignInUtils.doPostSignUp(account.getUsername(), request);
	// return "redirect:/";
	// }
}