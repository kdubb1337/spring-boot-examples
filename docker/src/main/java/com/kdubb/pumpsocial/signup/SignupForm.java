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

import javax.validation.constraints.Size;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.connect.UserProfile;

import com.kdubb.pumpsocial.util.Utils;

public class SignupForm {

	private String username;

	@Size(min = 6, message = "must be at least 6 characters")
	private String password;

	private String firstName;
	private String lastName;

	private static final Logger LOG = LogManager.getLogger(SignupForm.class);
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public static SignupForm fromProviderUser(UserProfile providerUser) {
		SignupForm form = new SignupForm();
		form.setFirstName(providerUser.getFirstName());
		form.setLastName(providerUser.getLastName());
		form.setUsername(providerUser.getUsername());
		
		LOG.info("providerUser --> " + Utils.toPrettyJson(providerUser));
		
		return form;
	}
}