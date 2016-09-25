package com.kdubb.pumpsocial.config;

public class SpringMvcInitializer {// extends AbstractAnnotationConfigDispatcherServletInitializer {

//	@Override
//	protected Class<?>[] getRootConfigClasses() {
//		return new Class<?>[] { DatabaseConfig.class, WebMvcConfig.class, SecurityConfig.class, SocialConfig.class };
//	}
//
//	@Override
//	protected Class<?>[] getServletConfigClasses() {
//		return null;
//	}
//
//	@Override
//	protected String[] getServletMappings() {
//		return new String[] { "/" };
//	}
//	
//	@Override
//	protected Filter[] getServletFilters() {
//		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
//		encodingFilter.setEncoding("UTF-8");
//		encodingFilter.setForceEncoding(true);
//		
//		DelegatingFilterProxy reconnectDelegate = new DelegatingFilterProxy("apiExceptionHandler");
//		
//		return new Filter[] { reconnectDelegate, encodingFilter, new HiddenHttpMethodFilter() };
//	}
}