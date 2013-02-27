package com.burgmeier.jerseyoauth2.api.client;

public interface IRegisteredClientApp {

	String getClientId();
	
	String getClientSecret();
	
	String getApplicationName();

	String getCallbackUrl();
	
	ClientType getClientType();
}
