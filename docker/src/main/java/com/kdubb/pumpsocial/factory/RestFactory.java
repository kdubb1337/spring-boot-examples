package com.kdubb.pumpsocial.factory;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Log;
import retrofit.RestAdapter.LogLevel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kdubb.pumpsocial.util.JacksonConverter;

public class RestFactory {
	
	private RestAdapter restAdapter;
	private LogLevel logLevel = LogLevel.FULL;
	
	private final String baseUrl;

    private static final Logger LOG = LogManager.getLogger(RestFactory.class);
	
	public RestFactory(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public LogLevel getLogLevel() {
		return logLevel;
	}

	private RestAdapter getRestAdaptor() {
		if (restAdapter == null)
			createRestAdaptor();

		return restAdapter;
	}

	private void createRestAdaptor() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		
		JacksonConverter converter = new JacksonConverter(mapper);
		
		RequestInterceptor interceptor = new RequestInterceptor() {
		    @Override
		    public void intercept(RequestFacade request) {
		        request.addHeader("Content-Type", "application/json");
		    }
		};
		
		restAdapter = new RestAdapter.Builder()
			.setRequestInterceptor(interceptor)
			.setEndpoint(baseUrl)
			.setConverter(converter)
			.setLog(new Log(){
				@Override
				public void log(String message) {
					if(message == null)
						return;
					
					// TODO turn this off when fix found for logging issue in Retrofit
					if(message.startsWith("<--- HTTP") || message.startsWith("---> HTTP"))
						LOG.info("Retrofit call [" + message + "]");
				}
			})
			.build();

		if(logLevel != null)
			restAdapter.setLogLevel(logLevel);
	}

	public <T> T create(Class<T> service) {
		return getRestAdaptor().create(service);
	}
}