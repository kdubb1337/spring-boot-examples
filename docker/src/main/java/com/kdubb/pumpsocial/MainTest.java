package com.kdubb.pumpsocial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kdubb.pumpsocial.domain.Account;

public class MainTest {

	public static final Logger LOG = LogManager.getLogger(MainTest.class);
	
	public static void main(String[] args) {
		List<Account> accounts = new ArrayList<>();
		
		for(int i = 1; i <= 100; i++) {
			Account account = new Account();
			account.setEmail("me" + i + "@goatmail.com");
			account.setFirstName("bob" + i) ;
			account.setNumPosts(i * 100);
			accounts.add(account);
		}
		
		Stream<Account> stream = accounts.stream();
		
		
		LOG.info("size=" + stream.mapToInt(a -> (int)a.getNumPosts()).count());
//		LOG.info("size=" + stream.);
	}
}
