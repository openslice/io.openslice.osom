package io.openslice.osom.partnerservices;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

//@Configuration
/**
 * @author ctranoris
 *
 * Since we need multiple clients to be created, we don;t use spring configuration, but they are created on demand
 * We implement here a Servlet integration
 * see: https://github.com/spring-projects/spring-security/blob/master/docs/manual/src/docs/asciidoc/_includes/servlet/oauth2/oauth2-client.adoc#oauth2Client-client-creds-grant
 */
public class GenericClient  {

	private static final transient Log log = LogFactory.getLog(GenericClient.class.getName());
	private String username;
	private String password;
	private String clientRegistrationId;
	
	
	OAuth2AuthorizedClientService clientService;
	private String baseUrl;
	
	/**
	 * Note: the constructor might change to support the instantiation of multiple clientRegistrations
	 * 
	 * @param username
	 * @param password
	 * @param clientRegistrationId
	 */
	public GenericClient(String username, String password, String clientRegistrationId, String baseUrl) {
		super();
		this.username = username;
		this.password = password;
		this.clientRegistrationId = clientRegistrationId;
		this.baseUrl = baseUrl;
	}

	public WebClient createWebClient() throws SSLException{
			

		InMemoryClientRegistrationRepository clientRegistrations = (InMemoryClientRegistrationRepository) this.clientRegistrations() ;
		OAuth2AuthorizedClientService clientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrations);
		OAuth2AuthorizedClientManager authorizedClientManager = this.authorizedClientManager(clientRegistrations, clientService);
				
		
		ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction =
				this.servletOAuth2AuthorizedClientExchangeFilterFunction(
						clientRegistrations,
						authorizedClientManager);
		
		ClientHttpConnector clientHttpConnector =  this.clientHttpConnector() ;
		
		return webClient(servletOAuth2AuthorizedClientExchangeFilterFunction, clientHttpConnector);		
	}
	
	
	//@Bean
	private WebClient webClient(
			ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction,
			ClientHttpConnector clientHttpConnector) {

		log.info("WebClientConfiguration.messageWebClient()");

		return WebClient.builder()
	        	 .exchangeStrategies( getExchangeStrategies() )
				.baseUrl( this.baseUrl )
				.clientConnector(clientHttpConnector)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.apply(servletOAuth2AuthorizedClientExchangeFilterFunction.oauth2Configuration())
				.filter(logRequest())
				.build();
	}
	
	private ExchangeStrategies getExchangeStrategies() {

		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build(); //spring.codec.max-in-memory-size=-1 ?? if use autoconfiguration
		return exchangeStrategies;
	}
	

	private ExchangeFilterFunction logRequest() {
		return (clientRequest, next) -> {
			log.info("Request: " + clientRequest.method() + ", " + clientRequest.url());
			clientRequest.headers()
					.forEach((name, values) -> values.forEach(value -> log.info("{" + name + "}={" + value + "}")));
			return next.exchange(clientRequest);
		};
	}

	//@Bean
	public ClientRegistrationRepository clientRegistrations() {

		log.info("WebClientConfiguration.clientRegistrations()");

		ClientRegistration clientRegistration = ClientRegistration
				.withRegistrationId("authOpensliceProvider")
				.clientId("osapiWebClientId")
				.clientSecret("secret")
				.scope("admin")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.tokenUri("http://portal.openslice.io/osapi-oauth-server/oauth/token")
				.build();

		return new InMemoryClientRegistrationRepository(clientRegistration);
	}

	  //@Bean
	public ServletOAuth2AuthorizedClientExchangeFilterFunction servletOAuth2AuthorizedClientExchangeFilterFunction(
			ClientRegistrationRepository clientRegistrations,
			
			OAuth2AuthorizedClientManager authorizedClientManager) {

		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager);

		// oauth.setDefaultOAuth2AuthorizedClient(true);
		//oauth.setDefaultClientRegistrationId("authOpensliceProvider");
		oauth.setDefaultClientRegistrationId( this.clientRegistrationId );
		
		// oauth.setAccessTokenExpiresSkew(Duration.ofSeconds(30));

		return oauth;
	}

	    //@Bean
	public ClientHttpConnector clientHttpConnector() throws SSLException {

		log.info("WebClientConfiguration.clientHttpConnector()");
		
		SslContext sslContext = SslContextBuilder
				.forClient()
				.trustManager()
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();

		TcpClient tcpClient = TcpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
				.doOnConnected(connection -> connection.addHandlerLast
						(new ReadTimeoutHandler(2)).addHandlerLast(new WriteTimeoutHandler(2))
						);

		return new ReactorClientHttpConnector(
   			 HttpClient.from(tcpClient)
   			 .secure( sslContextSpec -> sslContextSpec.sslContext(sslContext) )
   			 );
	}

	    //@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository, 
			OAuth2AuthorizedClientService clientService) {

		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				// .clientCredentials()
				.password().refreshToken().build();

		AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = 
				new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, clientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());
		return authorizedClientManager;
	}

	private Function<OAuth2AuthorizeRequest, Map<String, Object>> contextAttributesMapper() {
		return authorizeRequest -> {
			Map<String, Object> contextAttributes = Collections.emptyMap();
			HttpServletRequest servletRequest = authorizeRequest.getAttribute(HttpServletRequest.class.getName());
			// String username = "admin";//
			// servletRequest.getParameter(OAuth2ParameterNames.USERNAME);
			// String password =
			// "openslice";//servletRequest.getParameter(OAuth2ParameterNames.PASSWORD);
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				contextAttributes = new HashMap<>();

				// `PasswordOAuth2AuthorizedClientProvider` requires both attributes
				contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, this.username);
				contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, this.password);
			}
			return contextAttributes;
		};
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}