package com.burgmeier.jerseyoauth2.rs.impl.filter;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.ParameterStyle;
import org.apache.amber.oauth2.rs.request.OAuthAccessResourceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.burgmeier.jerseyoauth2.api.token.InvalidTokenException;
import com.burgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.burgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.burgmeier.jerseyoauth2.rs.impl.base.AbstractOAuth2Filter;
import com.burgmeier.jerseyoauth2.rs.impl.base.OAuth2FilterException;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

class OAuth20AuthenticationRequestFilter extends AbstractOAuth2Filter implements ContainerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(OAuth20AuthenticationRequestFilter.class);
	
	private Set<String> requiredScopes;
	private final IAccessTokenVerifier accessTokenVerifier;
	private ParameterStyle[] parameterStyles;
	
	public OAuth20AuthenticationRequestFilter(final IAccessTokenVerifier accessTokenVerifier, final IRSConfiguration configuration) {
		this.accessTokenVerifier = accessTokenVerifier;
		this.parameterStyles = configuration.getSupportedOAuthParameterStyles();
	}

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest) {
	
		try {
			OAuthAccessResourceRequest oauthRequest = new 
			        OAuthAccessResourceRequest(new WebRequestAdapter(containerRequest), parameterStyles);
			logger.debug("parse request successful");
		
			boolean secure = isRequestSecure(containerRequest);
			SecurityContext securityContext = filterOAuth2Request(oauthRequest, requiredScopes, secure);
			
			containerRequest.setSecurityContext(securityContext );
			logger.debug("set SecurityContext. User {}", securityContext.getUserPrincipal().getName());
			
			return containerRequest;
		} catch (OAuthSystemException e) {
			logger.error("Error in filter request", e);
			throw new WebApplicationException(buildAuthProblem());
		} catch (OAuthProblemException e) {
			logger.error("Error in filter request", e);
			throw new WebApplicationException(buildAuthProblem());			
		} catch (InvalidTokenException e) {
			logger.error("Error in filter request", e);
			throw new WebApplicationException(buildAuthProblem());			
		} catch (OAuth2FilterException e) {
			logger.error("Error in filter request", e);
			throw new WebApplicationException(e.getErrorResponse());			
		}
	}

	protected boolean isRequestSecure(ContainerRequest containerRequest) {
		URI requestUri = containerRequest.getRequestUri();
		String secureSSL = containerRequest.getHeaderValue("X-SSL-Secure");
		if (secureSSL!=null && "true".equals(secureSSL))
			return true;
		String scheme = requestUri.getScheme();
		return scheme!=null?scheme.equalsIgnoreCase("https"):false;
	}	
	
	void setRequiredScopes(String[] scopes) {
		this.requiredScopes = new HashSet<>(Arrays.asList(scopes));
	}

	@Override
	protected IAccessTokenVerifier getAccessTokenVerifier() {
		return accessTokenVerifier;
	}

}
