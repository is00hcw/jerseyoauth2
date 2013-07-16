package com.burgmeier.jerseyoauth2.testsuite.rs2.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.amber.oauth2.as.issuer.MD5Generator;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;

import com.burgmeier.jerseyoauth2.api.client.IAuthorizedClientApp;
import com.burgmeier.jerseyoauth2.api.user.IUser;
import com.burgmeier.jerseyoauth2.authsrv.api.client.ClientServiceException;
import com.burgmeier.jerseyoauth2.authsrv.api.client.ClientType;
import com.burgmeier.jerseyoauth2.authsrv.api.client.IPendingClientToken;
import com.burgmeier.jerseyoauth2.authsrv.api.client.IClientService;
import com.burgmeier.jerseyoauth2.authsrv.api.client.IRegisteredClientApp;
import com.burgmeier.jerseyoauth2.authsrv.impl.simple.SimpleAuthorizedClientApp;
import com.burgmeier.jerseyoauth2.authsrv.impl.simple.SimplePendingClientToken;
import com.burgmeier.jerseyoauth2.authsrv.impl.simple.SimpleRegisteredClient;
import com.google.inject.Inject;

public class TestClientService implements IClientService {

	private static final Map<String, IRegisteredClientApp> registeredClients = new HashMap<>();
	private static final Map<String, IAuthorizedClientApp> authorizedClients = new HashMap<>();
	private static final Map<String, IPendingClientToken> pendingAuth = new HashMap<>();
	private final MD5Generator md5Gen = new MD5Generator();

	@Inject
	public TestClientService()
	{
		registeredClients.put("AA", new SimpleRegisteredClient("AA", "BB", "CC", "/example1", ClientType.CONFIDENTIAL));
	}
	
	@Override
	public IPendingClientToken findPendingClientToken(String clientId, String clientSecret,
			String code) {
		String authKey = clientId+"#"+code;
		if (pendingAuth.containsKey(authKey))
		{
			IPendingClientToken clientAuthorization = pendingAuth.get(authKey);
			if (!clientAuthorization.getAuthorizedClient().isClientSecretValid(clientSecret))
				return null;
			else {
				pendingAuth.remove(authKey);
				return clientAuthorization;
			}
		} else
			return null;
		
	}

	@Override
	public IAuthorizedClientApp authorizeClient(IUser user, IRegisteredClientApp clientApp,
			Set<String> allowedScopes) {
		IAuthorizedClientApp authClientApp = new SimpleAuthorizedClientApp(clientApp, user, allowedScopes);
		String key = user.getName()+"#"+clientApp.getClientId();
		authorizedClients.put(key, authClientApp);
		return authClientApp;
	}	
	
	@Override
	public IAuthorizedClientApp isAuthorized(IUser user, String clientId,
			Set<String> scopes) {
		
		String key = user.getName()+"#"+clientId;
		if (authorizedClients.containsKey(key))
		{
			IAuthorizedClientApp clientApp = authorizedClients.get(key);
			return clientApp;
		} else
			return null;
	}

	@Override
	public IRegisteredClientApp registerClient(String appName, String callbackUrl, ClientType clientType) throws ClientServiceException {
		try {
			String clientId = md5Gen.generateValue();
			String clientSecret = md5Gen.generateValue();
			SimpleRegisteredClient client = new SimpleRegisteredClient(clientId, clientSecret, appName, callbackUrl, clientType);
			registeredClients.put(clientId, client);
			return client;
		} catch (OAuthSystemException e) {
			throw new ClientServiceException(e);
		}
	}

	@Override
	public IRegisteredClientApp getRegisteredClient(String clientId) {
		return registeredClients.get(clientId);
	}

	@Override
	public IPendingClientToken createPendingClientToken(
			IAuthorizedClientApp clientApp) {
		try {
			String code = md5Gen.generateValue();
			IPendingClientToken clientAuth = new SimplePendingClientToken(code, clientApp);
			String authKey = clientApp.getClientId()+"#"+code;
			pendingAuth.put(authKey, clientAuth);
			return clientAuth;
		} catch (OAuthSystemException e) {
			return null; //TODO handle exception
		}
	}

}