package com.kdubb.pumpsocial.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.kdubb.pumpsocial.ConfigConstants;

@Configuration
public class PoolConfig {

	@Value(ConfigConstants.POOL_MAX_SIZE)
	private int maxSize;

	@Value(ConfigConstants.POOL_QUEUE_SIZE)
	private int queueSize;

	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setMaxPoolSize(maxSize);
		executor.setQueueCapacity(queueSize);

		return executor;
	}
}