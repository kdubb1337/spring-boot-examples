package com.kdubb.pumpsocial.controller.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.connect.web.ConnectInterceptor;
import org.springframework.social.connect.web.ConnectSupport;
import org.springframework.social.connect.web.DisconnectInterceptor;
import org.springframework.social.connect.web.HttpSessionSessionStrategy;
import org.springframework.social.connect.web.SessionStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * Generic UI controller for managing the account-to-service-provider connection flow.
 * <ul>
 * <li>GET /connect/{providerId} - Get a web page showing connection status to {providerId}.</li>
 * <li>POST /connect/{providerId} - Initiate an connection with {providerId}.</li>
 * <li>GET /connect/{providerId}?oauth_verifier||code - Receive {providerId} authorization callback and establish the connection.</li>
 * <li>DELETE /connect/{providerId} - Disconnect from {providerId}.</li>
 * </ul>
 * 
 * @author Keith Donald
 * @author Craig Walls
 * @author Roy Clarkson
 */
@Controller
@RequestMapping("/connect2")
public class ConnectController2 implements InitializingBean {

	private final static Logger LOG = LogManager.getLogger(ConnectController2.class);

	private final ConnectionFactoryLocator connectionFactoryLocator;

	private final ConnectionRepository connectionRepository;

	private final MultiValueMap<Class<?>, ConnectInterceptor<?>> connectInterceptors = new LinkedMultiValueMap<Class<?>, ConnectInterceptor<?>>();

	private final MultiValueMap<Class<?>, DisconnectInterceptor<?>> disconnectInterceptors = new LinkedMultiValueMap<Class<?>, DisconnectInterceptor<?>>();

	private ConnectSupport connectSupport;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private String viewPath = "connect2/";

	private SessionStrategy sessionStrategy = new HttpSessionSessionStrategy();

	/**
	 * Constructs a ConnectController.
	 * 
	 * @param connectionFactoryLocator
	 *            the locator for {@link ConnectionFactory} instances needed to establish connections
	 * @param connectionRepository
	 *            the current user's {@link ConnectionRepository} needed to persist connections; must be a proxy to a request-scoped bean
	 */
	@Inject
	public ConnectController2(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository) {
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.connectionRepository = connectionRepository;
	}

	/**
	 * Configure the list of connect interceptors that should receive callbacks during the connection process. Convenient when an instance
	 * of this class is configured using a tool that supports JavaBeans-based configuration.
	 * 
	 * @param interceptors
	 *            the connect interceptors to add
	 * @deprecated Use {@link #setConnectInterceptors(List)} instead.
	 */
	@Deprecated
	public void setInterceptors(List<ConnectInterceptor<?>> interceptors) {
		setConnectInterceptors(interceptors);
	}

	/**
	 * Configure the list of connect interceptors that should receive callbacks during the connection process. Convenient when an instance
	 * of this class is configured using a tool that supports JavaBeans-based configuration.
	 * 
	 * @param interceptors
	 *            the connect interceptors to add
	 */
	public void setConnectInterceptors(List<ConnectInterceptor<?>> interceptors) {
		for (ConnectInterceptor<?> interceptor : interceptors) {
			addInterceptor(interceptor);
		}
	}

	/**
	 * Configure the list of discconnect interceptors that should receive callbacks when connections are removed. Convenient when an
	 * instance of this class is configured using a tool that supports JavaBeans-based configuration.
	 * 
	 * @param interceptors
	 *            the connect interceptors to add
	 */
	public void setDisconnectInterceptors(List<DisconnectInterceptor<?>> interceptors) {
		for (DisconnectInterceptor<?> interceptor : interceptors) {
			addDisconnectInterceptor(interceptor);
		}
	}

	/**
	 * Configures the base secure URL for the application this controller is being used in e.g. <code>https://myapp.com</code>. Defaults to
	 * null. If specified, will be used to generate OAuth callback URLs. If not specified, OAuth callback URLs are generated from web
	 * request info. You may wish to set this property if requests into your application flow through a proxy to your application server. In
	 * this case, the request URI may contain a scheme, host, and/or port value that points to an internal server not appropriate for an
	 * external callback URL. If you have this problem, you can set this property to the base external URL for your application and it will
	 * be used to construct the callback URL instead.
	 * 
	 * @param applicationUrl
	 *            the application URL value
	 */
	public void setApplicationUrl(String applicationUrl) {
		connectSupport.setApplicationUrl(applicationUrl);
	}

	/**
	 * Sets the path to connection status views. Prepended to provider-specific views (e.g., "connect/facebookConnected") to create the
	 * complete view name. Defaults to "connect/".
	 * 
	 * @param viewPath
	 *            The path to connection status views.
	 */
	public void setViewPath(String viewPath) {
		this.viewPath = viewPath;
	}

	/**
	 * Sets a strategy to use when persisting information that is to survive past the boundaries of a request. The default strategy is to
	 * set the data as attributes in the HTTP Session.
	 * 
	 * @param sessionStrategy
	 *            the session strategy.
	 */
	public void setSessionStrategy(SessionStrategy sessionStrategy) {
		this.sessionStrategy = sessionStrategy;
	}

	/**
	 * Adds a ConnectInterceptor to receive callbacks during the connection process. Useful for programmatic configuration.
	 * 
	 * @param interceptor
	 *            the connect interceptor to add
	 */
	public void addInterceptor(ConnectInterceptor<?> interceptor) {
		Class<?> serviceApiType = GenericTypeResolver.resolveTypeArgument(interceptor.getClass(), ConnectInterceptor.class);
		connectInterceptors.add(serviceApiType, interceptor);
	}

	/**
	 * Adds a DisconnectInterceptor to receive callbacks during the disconnection process. Useful for programmatic configuration.
	 * 
	 * @param interceptor
	 *            the connect interceptor to add
	 */
	public void addDisconnectInterceptor(DisconnectInterceptor<?> interceptor) {
		Class<?> serviceApiType = GenericTypeResolver.resolveTypeArgument(interceptor.getClass(), DisconnectInterceptor.class);
		disconnectInterceptors.add(serviceApiType, interceptor);
	}

	/**
	 * Render the status of connections across all providers to the user as HTML in their web browser.
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String connectionStatus(NativeWebRequest request, Model model) {
		setNoCache(request);
		processFlash(request, model);
		Map<String, List<Connection<?>>> connections = connectionRepository.findAllConnections();
		model.addAttribute("providerIds", connectionFactoryLocator.registeredProviderIds());
		model.addAttribute("connectionMap", connections);
		return connectView();
	}

	/**
	 * Render the status of the connections to the service provider to the user as HTML in their web browser.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.GET)
	public String connectionStatus(@PathVariable String providerId, NativeWebRequest request, Model model) {
		setNoCache(request);
		processFlash(request, model);
		List<Connection<?>> connections = connectionRepository.findConnections(providerId);
		setNoCache(request);
		if (connections.isEmpty()) {
			return connectView(providerId);
		} else {
			model.addAttribute("connections", connections);
			return connectedView(providerId);
		}
	}

	/**
	 * Process a connect form submission by commencing the process of establishing a connection to the provider on behalf of the member. For
	 * OAuth1, fetches a new request token from the provider, temporarily stores it in the session, then redirects the member to the
	 * provider's site for authorization. For OAuth2, redirects the user to the provider's site for authorization.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.POST)
	public RedirectView connect(@PathVariable String providerId, NativeWebRequest request) {
		LOG.info("Connecting with Google+");

		ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId);
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
		preConnect(connectionFactory, parameters, request);

		// Request offline access for Google+. Will allow a refreshToken
		parameters.put("access_type", Arrays.asList("offline"));

		try {
			return new RedirectView(connectSupport.buildOAuthUrl(connectionFactory, request, parameters));
		} catch (Exception e) {
			sessionStrategy.setAttribute(request, PROVIDER_ERROR_ATTRIBUTE, e);
			return connectionStatusRedirect(providerId, request);
		}
	}

	/**
	 * Process the authorization callback from an OAuth 1 service provider. Called after the user authorizes the connection, generally done
	 * by having he or she click "Allow" in their web browser at the provider's site. On authorization verification, connects the user's
	 * local account to the account they hold at the service provider Removes the request token from the session since it is no longer valid
	 * after the connection is established.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.GET, params = "oauth_token")
	public RedirectView oauth1Callback(@PathVariable String providerId, NativeWebRequest request) {
		try {
			OAuth1ConnectionFactory<?> connectionFactory = (OAuth1ConnectionFactory<?>) connectionFactoryLocator.getConnectionFactory(providerId);
			Connection<?> connection = connectSupport.completeConnection(connectionFactory, request);
			addConnection(connection, connectionFactory, request);
		} catch (Exception e) {
			sessionStrategy.setAttribute(request, PROVIDER_ERROR_ATTRIBUTE, e);
			LOG.warn("Exception while handling OAuth1 callback (" + e.getMessage() + "). Redirecting to " + providerId
					+ " connection status page.");
		}
		return connectionStatusRedirect(providerId, request);
	}

	/**
	 * Process the authorization callback from an OAuth 2 service provider. Called after the user authorizes the connection, generally done
	 * by having he or she click "Allow" in their web browser at the provider's site. On authorization verification, connects the user's
	 * local account to the account they hold at the service provider.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.GET, params = "code")
	public RedirectView oauth2Callback(@PathVariable String providerId, NativeWebRequest request) {
		try {
			OAuth2ConnectionFactory<?> connectionFactory = (OAuth2ConnectionFactory<?>) connectionFactoryLocator.getConnectionFactory(providerId);
			Connection<?> connection = connectSupport.completeConnection(connectionFactory, request);
			addConnection(connection, connectionFactory, request);
		} catch (Exception e) {
			sessionStrategy.setAttribute(request, PROVIDER_ERROR_ATTRIBUTE, e);
			LOG.warn("Exception while handling OAuth2 callback (" + e.getMessage() + "). Redirecting to " + providerId
					+ " connection status page.");
		}
		return connectionStatusRedirect(providerId, request);
	}

	/**
	 * Process an error callback from an OAuth 2 authorization as described at http://tools.ietf.org/html/rfc6749#section-4.1.2.1. Called
	 * after upon redirect from an OAuth 2 provider when there is some sort of error during authorization, typically because the user denied
	 * authorization.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.GET, params = "error")
	public RedirectView oauth2ErrorCallback(@PathVariable String providerId, @RequestParam("error") String error,
			@RequestParam(value = "error_description", required = false) String errorDescription,
			@RequestParam(value = "error_uri", required = false) String errorUri, NativeWebRequest request) {
		Map<String, String> errorMap = new HashMap<String, String>();
		errorMap.put("error", error);
		if (errorDescription != null) {
			errorMap.put("errorDescription", errorDescription);
		}
		if (errorUri != null) {
			errorMap.put("errorUri", errorUri);
		}
		sessionStrategy.setAttribute(request, AUTHORIZATION_ERROR_ATTRIBUTE, errorMap);
		return connectionStatusRedirect(providerId, request);
	}

	/**
	 * Remove all provider connections for a user account. The user has decided they no longer wish to use the service provider from this
	 * application. Note: requires {@link HiddenHttpMethodFilter} to be registered with the '_method' request parameter set to 'DELETE' to
	 * convert web browser POSTs to DELETE requests.
	 */
	@RequestMapping(value = "/{providerId}", method = RequestMethod.DELETE)
	public RedirectView removeConnections(@PathVariable String providerId, NativeWebRequest request) {
		ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId);
		preDisconnect(connectionFactory, request);
		connectionRepository.removeConnections(providerId);
		postDisconnect(connectionFactory, request);
		return connectionStatusRedirect(providerId, request);
	}

	/**
	 * Remove a single provider connection associated with a user account. The user has decided they no longer wish to use the service
	 * provider account from this application. Note: requires {@link HiddenHttpMethodFilter} to be registered with the '_method' request
	 * parameter set to 'DELETE' to convert web browser POSTs to DELETE requests.
	 */
	@RequestMapping(value = "/{providerId}/{providerUserId}", method = RequestMethod.DELETE)
	public RedirectView removeConnection(@PathVariable String providerId, @PathVariable String providerUserId, NativeWebRequest request) {
		ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId);
		preDisconnect(connectionFactory, request);
		connectionRepository.removeConnection(new ConnectionKey(providerId, providerUserId));
		postDisconnect(connectionFactory, request);
		return connectionStatusRedirect(providerId, request);
	}

	// subclassing hooks
	/**
	 * Returns the view name of a general connection status page, typically displaying the user's connection status for all providers.
	 * Defaults to "/connect/status". May be overridden to return a custom view name.
	 */
	protected String connectView() {
		return getViewPath() + "status";
	}

	/**
	 * Returns the view name of a page to display for a provider when the user is not connected to the provider. Typically this page would
	 * offer the user an opportunity to create a connection with the provider. Defaults to "connect/{providerId}Connect". May be overridden
	 * to return a custom view name.
	 * 
	 * @param providerId
	 *            the ID of the provider to display the connection status for.
	 */
	protected String connectView(String providerId) {
		return getViewPath() + providerId + "Connect";
	}

	/**
	 * Returns the view name of a page to display for a provider when the user is connected to the provider. Typically this page would allow
	 * the user to disconnect from the provider. Defaults to "connect/{providerId}Connected". May be overridden to return a custom view
	 * name.
	 * 
	 * @param providerId
	 *            the ID of the provider to display the connection status for.
	 */
	protected String connectedView(String providerId) {
		return getViewPath() + providerId + "Connected";
	}

	/**
	 * Returns a RedirectView with the URL to redirect to after a connection is created or deleted. Defaults to "/connect/{providerId}"
	 * relative to DispatcherServlet's path. May be overridden to handle custom redirection needs.
	 * 
	 * @param providerId
	 *            the ID of the provider for which a connection was created or deleted.
	 * @param request
	 *            the NativeWebRequest used to access the servlet path when constructing the redirect path.
	 */
	protected RedirectView connectionStatusRedirect(String providerId, NativeWebRequest request) {
		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
		String path = "/connect/" + providerId + getPathExtension(servletRequest);
		if (prependServletPath(servletRequest)) {
			path = servletRequest.getServletPath() + path;
		}
		return new RedirectView(path, true);
	}

	// From InitializingBean
	public void afterPropertiesSet() throws Exception {
		this.connectSupport = new ConnectSupport(sessionStrategy);
	}

	// internal helpers

	private boolean prependServletPath(HttpServletRequest request) {
		return !this.urlPathHelper.getPathWithinServletMapping(request).equals("");
	}

	/*
	 * Determines the path extension, if any. Returns the extension, including the period at the beginning, or an empty string if there is
	 * no extension. This makes it possible to append the returned value to a path even if there is no extension.
	 */
	private String getPathExtension(HttpServletRequest request) {
		String fileName = WebUtils.extractFullFilenameFromUrlPath(request.getRequestURI());
		String extension = StringUtils.getFilenameExtension(fileName);
		return extension != null ? "." + extension : "";
	}

	private String getViewPath() {
		return viewPath;
	}

	private void addConnection(Connection<?> connection, ConnectionFactory<?> connectionFactory, WebRequest request) {

		try {
			connectionRepository.addConnection(connection);
			postConnect(connectionFactory, connection, request);
		} catch (DuplicateConnectionException e) {
			sessionStrategy.setAttribute(request, DUPLICATE_CONNECTION_ATTRIBUTE, e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void preConnect(ConnectionFactory<?> connectionFactory, MultiValueMap<String, String> parameters, WebRequest request) {
		for (ConnectInterceptor interceptor : interceptingConnectionsTo(connectionFactory)) {
			interceptor.preConnect(connectionFactory, parameters, request);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void postConnect(ConnectionFactory<?> connectionFactory, Connection<?> connection, WebRequest request) {
		for (ConnectInterceptor interceptor : interceptingConnectionsTo(connectionFactory)) {
			interceptor.postConnect(connection, request);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void preDisconnect(ConnectionFactory<?> connectionFactory, WebRequest request) {
		for (DisconnectInterceptor interceptor : interceptingDisconnectionsTo(connectionFactory)) {
			interceptor.preDisconnect(connectionFactory, request);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void postDisconnect(ConnectionFactory<?> connectionFactory, WebRequest request) {
		for (DisconnectInterceptor interceptor : interceptingDisconnectionsTo(connectionFactory)) {
			interceptor.postDisconnect(connectionFactory, request);
		}
	}

	private List<ConnectInterceptor<?>> interceptingConnectionsTo(ConnectionFactory<?> connectionFactory) {
		Class<?> serviceType = GenericTypeResolver.resolveTypeArgument(connectionFactory.getClass(), ConnectionFactory.class);
		List<ConnectInterceptor<?>> typedInterceptors = connectInterceptors.get(serviceType);
		if (typedInterceptors == null) {
			typedInterceptors = Collections.emptyList();
		}
		return typedInterceptors;
	}

	private List<DisconnectInterceptor<?>> interceptingDisconnectionsTo(ConnectionFactory<?> connectionFactory) {
		Class<?> serviceType = GenericTypeResolver.resolveTypeArgument(connectionFactory.getClass(), ConnectionFactory.class);
		List<DisconnectInterceptor<?>> typedInterceptors = disconnectInterceptors.get(serviceType);
		if (typedInterceptors == null) {
			typedInterceptors = Collections.emptyList();
		}
		return typedInterceptors;
	}

	private void processFlash(WebRequest request, Model model) {
		convertSessionAttributeToModelAttribute(DUPLICATE_CONNECTION_ATTRIBUTE, request, model);
		convertSessionAttributeToModelAttribute(PROVIDER_ERROR_ATTRIBUTE, request, model);
		model.addAttribute(AUTHORIZATION_ERROR_ATTRIBUTE, sessionStrategy.getAttribute(request, AUTHORIZATION_ERROR_ATTRIBUTE));
		sessionStrategy.removeAttribute(request, AUTHORIZATION_ERROR_ATTRIBUTE);
	}

	private void convertSessionAttributeToModelAttribute(String attributeName, WebRequest request, Model model) {
		if (sessionStrategy.getAttribute(request, attributeName) != null) {
			model.addAttribute(attributeName, Boolean.TRUE);
			sessionStrategy.removeAttribute(request, attributeName);
		}
	}

	private void setNoCache(NativeWebRequest request) {
		HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
		if (response != null) {
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 1L);
			response.setHeader("Cache-Control", "no-cache");
			response.addHeader("Cache-Control", "no-store");
		}
	}

	private static final String DUPLICATE_CONNECTION_ATTRIBUTE = "social_addConnection_duplicate";

	private static final String PROVIDER_ERROR_ATTRIBUTE = "social_provider_error";

	private static final String AUTHORIZATION_ERROR_ATTRIBUTE = "social_authorization_error";

}
