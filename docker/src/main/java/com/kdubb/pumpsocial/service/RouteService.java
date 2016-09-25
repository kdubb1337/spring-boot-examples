package com.kdubb.pumpsocial.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.safe.RouteRepoSafe;
import com.kdubb.pumpsocial.repository.safe.SocialConnectionRepoSafe;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class RouteService {

	@Autowired
	private RouteRepoSafe routeRepo;

	@Autowired
	private SocialConnectionRepoSafe socialConnectionRepo;

	@Autowired
	private SocialConnectionService socialConnectionService;

	@Autowired
	private PostService postService;

	@Autowired
	private WebsocketService websocketService;

	private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

	public Collection<Route> getRoutesForUser(final String userId) {
		List<SocialConnection> connections = socialConnectionRepo.findByUserId(userId);

		Map<String, Route> result = new HashMap<>();

		connections.parallelStream().forEach(connection -> {
			Set<Route> routes = routeRepo.findByTargets(connection);

			for (Route route : routes)
				if (!result.containsKey(route.getId().toString()))
					result.put(route.getId().toString(), route);
		});

		for (Route route : result.values()) {
			setConnectionName(route.getSource());

			for (SocialConnection connection : route.getTargets())
				setConnectionName(connection);
		}

		return result.values();
	}

	private void setConnectionName(SocialConnection connection) {
		if (connection == null)
			return;

		String name = socialConnectionService.getSocialConnectionName(connection);
		connection.setName(name);
	}

	public void saveRoute(Route route) {
		saveRoute(route, true);
	}
	
	// TODO need to save the SocialConnections as saving is not cascaded
	public void saveRoute(Route route, boolean addativeTargets) {

		Route existing = null;

		if (route.getId() != null) {
			existing = routeRepo.findBySource(route.getSource());
		} else {
			existing = routeRepo.findOne(route.getId());
		}

		if (existing == null) {
			LOG.info("Creating a new route");
			saveRouteAfterMerge(route);
			websocketService.updateState(route.getSource().getUserId());
			return;
		}

		LOG.info("existing Targets --> " + Utils.toPrettyJson(existing.getTargets()));
		route.merge(existing, addativeTargets);
		LOG.info("new Targets --> " + Utils.toPrettyJson(existing.getTargets()));
		saveRouteAfterMerge(existing);
		websocketService.updateState(route.getSource().getUserId());
	}

	private void saveRouteAfterMerge(Route route) {
		// TODO need to determine if we need to make an RSS connection or reuse an exising social connection
		// socialConnectionService.save(route.getSource());
		// socialConnectionService.save(route.getTargets());
		routeRepo.save(route);
	}

	public List<Route> findAll() throws DataAccessException {
		return routeRepo.findAll();
	}

	public Route findOne(ObjectId id) {
		return routeRepo.findOne(id);
	}

	public void delete(ObjectId id) {
		Route route = findOne(id);
		delete(route);
	}

	public void delete(Set<Route> routes) {
		if (CollectionUtils.isEmpty(routes))
			return;

		for (Route route : routes)
			delete(route);
	}

	public void delete(Route route) {
		if (route == null || route.getId() == null)
			return;

		SocialConnection source = route.getSource();
		String userId = source.getUserId();

		if (SocialConnectionType.rss.equals(source.getType()))
			socialConnectionRepo.delete(source);

		for (SocialConnection connection : route.getTargets())
			if (connection != null && SocialConnectionType.rss.equals(connection.getType()))
				socialConnectionRepo.delete(connection);

		routeRepo.delete(route);
		websocketService.updateState(userId);
	}

	public int triggerRoute(ObjectId id) {
		Route route = routeRepo.findOne(id);
		return postService.executeRoute(route);
	}

	public int triggerRoute(String userId) {
		int result = 0;
		Collection<Route> routes = getRoutesForUser(userId);

		for (Route route : routes) {
			result += triggerRoute(route.getId());
		}

		return result;
	}

	public Route updateRoute(Route route, String userId) {
		if (route == null || route.getId() == null)
			return null;

		Route existing = findOne(route.getId());

		if (existing == null) {
			LOG.error("could not find route for id={" + route.getId() + "}");
			return null;
		}

		if (SocialConnectionType.rss.equals(route.getSource().getType()))
			route.getSource().setUserId(null);
		else
			route.getSource().setUserId(userId);

		Map<String, SocialConnection> currentConnections = new HashMap<String, SocialConnection>();

		for (SocialConnection connection : route.getTargets()) {
			if (connection == null)
				continue;

			connection.setUserId(userId);

			if (connection.getId() != null) {
				connection = socialConnectionService.merge(connection, userId);
				currentConnections.put(connection.getId().toString(), connection);
			}
		}

		for (SocialConnection existingConn : existing.getTargets()) {
			if (existingConn == null || existingConn.getId() == null || !currentConnections.containsKey(existingConn.getId().toString()))
				socialConnectionRepo.delete(existingConn);
		}

		route.merge(existing);
		saveRoute(existing);

		return existing;
	}

	public Route findBySource(SocialConnection socialConnection) {
		return routeRepo.findBySource(socialConnection);
	}

	public Set<Route> findByTargets(SocialConnection socialConnection) {
		return routeRepo.findByTargets(socialConnection);
	}
}