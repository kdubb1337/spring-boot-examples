package com.kdubb.pumpsocial.domain;

import javax.persistence.Transient;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Account extends MongoBase {

	private String displayName;
	
	private String username;
	private String password;
	private String email;
	
	private String firstName;
	private String lastName;
	
	@Transient
	private long numPosts;
	
	public long getNumPosts() {
		return numPosts;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setNumPosts(long numPosts) {
		this.numPosts = numPosts;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void merge(Account account) {
		if(account == null)
			return;
		
		if(account.getDisplayName() != null)
			setDisplayName(account.getDisplayName());
		
		if(account.getFirstName() != null)
			setFirstName(account.getFirstName());
		
		if(account.getLastName() != null)
			setLastName(account.getLastName());
		
		if(account.getEmail() != null)
			setEmail(account.getEmail());
	}
}