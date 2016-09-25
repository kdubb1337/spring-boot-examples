package com.kdubb.pumpsocial.service;

import java.util.concurrent.Callable;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class RepeaterService {

	private static final int DEFAULT_RETRIES = 5;
	private static final Logger LOG = LogManager.getLogger(RepeaterService.class);
	
	public void retryIfNecessary(Runnable runnable) {
		if(runnable == null)
			return;
		
		retryIfNecessary(runnable, DEFAULT_RETRIES, 1);
	}
	
	public void retryIfNecessary(Runnable call, int numTries) {
		if(call == null || numTries < 1)
			return;
		
		retryIfNecessary(call, numTries, 1);
	}
	
	private void retryIfNecessary(Runnable call, int numTries, int currentTry) {
		try {
			call.run();
		}
		catch(Exception e) {
			if(currentTry >= numTries) {
				LOG.error("Failed on try {" + currentTry + "}. Quitting!", e);
				return;
			}
			
			LOG.warn("Failed on try {" + currentTry + "}");
			
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e1) {
				LOG.error("Failed to sleep", e1);
			}
			
			retryIfNecessary(call, numTries, currentTry + 1);
		}
	}
	
	public <T> T retryIfNecessary(Callable<T> call) {
		if(call == null)
			return null;
		
		return retryIfNecessary(call, DEFAULT_RETRIES, 1);
	}
	
	public <T> T retryIfNecessary(Callable<T> call, int numTries) {
		if(call == null || numTries < 1)
			return null;
		
		return retryIfNecessary(call, numTries, 1);
	}
	
	private <T> T retryIfNecessary(Callable<T> call, int numTries, int currentTry) {
		try {
			return call.call();
		}
		catch(Exception e) {
			if(currentTry >= numTries) {
				LOG.error("Failed on try {" + currentTry + "}. Quitting!", e);
				return null;
			}
			
			LOG.warn("Failed on try {" + currentTry + "}");
			
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e1) {
				LOG.error("Failed to sleep", e1);
			}
			
			return retryIfNecessary(call, numTries, currentTry + 1);
		}
	}
}