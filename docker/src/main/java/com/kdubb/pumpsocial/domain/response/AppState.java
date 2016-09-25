package com.kdubb.pumpsocial.domain.response;

import java.util.Collection;
import java.util.List;

import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;

public class AppState {

	private List<SocialConnection> connectedNetworks;
	private Collection<Route> routes;

	public Collection<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(Collection<Route> routes) {
		this.routes = routes;
	}

	public List<SocialConnection> getConnectedNetworks() {
		return connectedNetworks;
	}

	public void setConnectedNetworks(List<SocialConnection> connectedNetworks) {
		this.connectedNetworks = connectedNetworks;
	}
}
