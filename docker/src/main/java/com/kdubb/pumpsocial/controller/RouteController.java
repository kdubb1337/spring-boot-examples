package com.kdubb.pumpsocial.controller;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.processor.AbstractImporter;
import com.kdubb.pumpsocial.service.PostService;
import com.kdubb.pumpsocial.service.ProcessorService;
import com.kdubb.pumpsocial.service.RouteService;
import com.kdubb.pumpsocial.service.SocialConnectionService;
import com.kdubb.pumpsocial.util.Utils;

@RestController
@RequestMapping("/route")
public class RouteController {

	@Inject
	private PostService postService;

	@Inject
	private ProcessorService processorService;

	@Inject
	private RouteService routeService;

	@Inject
	private SocialConnectionService socialConnectionService;

	private static final Logger LOG = LoggerFactory.getLogger(RouteController.class);

	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<Boolean> createRoute(@RequestBody Route route, Principal currentUser) {
		// Ensure UserId is set properly
		String userId = currentUser.getName();

		if (SocialConnectionType.rss.equals(route.getSource().getType()))
			route.getSource().setUserId(null);
		else
			route.getSource().setUserId(userId);

		if (StringUtils.isBlank(route.getSource().getName()))
			route.getSource().setName(socialConnectionService.getSocialConnectionName(route.getSource()));

		for (SocialConnection conn : route.getTargets()) {
			conn.setUserId(userId);

			if (StringUtils.isBlank(conn.getName()))
				conn.setName(socialConnectionService.getSocialConnectionName(conn));
		}

		routeService.saveRoute(route);
		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/addTarget", method = RequestMethod.POST)
	public ResponseEntity<Boolean> addTarget(@RequestBody SocialConnection socialConnection, Principal currentUser) {
		// Ensure UserId is set properly
		String userId = currentUser.getName();

		List<SocialConnection> existingConnections = socialConnectionService.findByUserId(userId);

		for (SocialConnection connection : existingConnections) {
			// Don't repost within the same network (for now)
			if (connection.getType().equals(socialConnection.getType()))
				continue;

			// Add routes from all parent accounts to each other
			if (connection.getParent() == null) {
				if (connection.getIsSource() && socialConnection.getIsTarget())
					LOG.info("ADD " + connection.getType() + " --> " + socialConnection.getName());
				socialConnectionService.addToRoute(connection, socialConnection);
			}
		}

		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/removeTarget", method = RequestMethod.POST)
	public ResponseEntity<Boolean> removeTarget(@RequestBody SocialConnection socialConnection, Principal currentUser) {
		Set<Route> routes = routeService.findByTargets(socialConnection);

		for (Route route : routes) {
			if (route.getTargets().size() <= 1) {
				routeService.delete(route);
				LOG.info("DELETE ROUTE --> " + Utils.toPrettyJson(route));
			} else {
				SocialConnection existing = null;

				for (SocialConnection curr : route.getTargets()) {
					if (curr.getId().equals(socialConnection.getId())) {
						existing = curr;
						break;
					}
				}

				if (existing != null) {
					route.getTargets().remove(existing);
					routeService.saveRoute(route);
					LOG.info("REMOVE FROM ROUTE --> " + Utils.toPrettyJson(route));
				}
			}
		}

		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/removeTarget", method = RequestMethod.GET)
	public ResponseEntity<Boolean> removeTarget(@RequestParam String targetId, @RequestParam String routeId, Principal currentUser) {
		Route route = routeService.findOne(new ObjectId(routeId));

		if (!route.getSource().getUserId().equals(currentUser.getName())) {
			LOG.error("Cannot removeTarget when not the owner of the route. route --> " + Utils.toPrettyJson(route));
			return Utils.httpError(false);
		}

		Set<SocialConnection> toRemove = route.getTargets()
				.stream()
				.filter(t -> t == null || t.getId() == null || t.getId().toString().equals(targetId))
				.collect(Collectors.toSet());

		LOG.info("toRemove --> {} " + Utils.toPrettyJson(toRemove), targetId);

		for (SocialConnection target : toRemove) {
			route.getTargets().remove(target);
		}

		// LOG.info("route(removeTarget) --> " + Utils.toPrettyJson(route));

		if (CollectionUtils.isEmpty(route.getTargets())) {
			routeService.delete(route);
		} else {
			routeService.saveRoute(route, false);
		}

		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/addTarget", method = RequestMethod.GET)
	public ResponseEntity<Boolean> addTarget(@RequestParam String targetId, @RequestParam String routeId, Principal currentUser) {
		Route route = routeService.findOne(new ObjectId(routeId));

		if (!route.getSource().getUserId().equals(currentUser.getName())) {
			LOG.error("Cannot addTarget when not the owner of the route. route --> " + Utils.toPrettyJson(route));
			return Utils.httpError(false);
		}

		SocialConnection target = socialConnectionService.findOne(targetId);
		route.getTargets().add(target);

		LOG.info("route(addTarget) --> " + Utils.toPrettyJson(route));
		routeService.saveRoute(route);

		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity<Boolean> updateRoute(@RequestBody Route route, Principal currentUser) {
		LOG.info("updateRoute=" + Utils.toPrettyJson(route));

		Route result = routeService.updateRoute(route, currentUser.getName());
		LOG.info("result=" + Utils.toPrettyJson(result));

		if (result == null)
			return Utils.httpError(false);

		return Utils.httpOK(true);
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Collection<Route>> getRoutes(Principal currentUser) {
		String userId = currentUser.getName();
		LOG.info("start getRoutes()");
		Collection<Route> routes = routeService.getRoutesForUser(userId);
		LOG.info("route size --> {}", routes.size());

		for (Route route : routes) {
			SocialConnection source = route.getSource();
			route.getTargets().remove(null);

			for (SocialConnection target : route.getTargets()) {
				if (source == null) {
					LOG.error("Route with empty source! --> " + Utils.toPrettyJson(route));
					continue;
				}

				if (target == null) {
					LOG.error("Route with empty target! --> " + Utils.toPrettyJson(route));
					continue;
				}

				LOG.info(source.getType() + " [" + source.getName() + "] --> " + target.getType() + " [" + target.getName() + "]");
			}
		}
		LOG.info("end getRoutes()");

		return Utils.httpOK(routes);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public ResponseEntity<Boolean> deleteRoute(@RequestBody ObjectId id) {
		routeService.delete(id);
		return Utils.httpOK(true);
	}

	// @RequestMapping(value = "/trigger", method = RequestMethod.GET)
	// public ResponseEntity<Boolean> triggerRoutes(Principal currentUser) {
	// String userId = currentUser.getName();
	//
	// // routeService.getRoutesForUser(userId)
	// // .forEach(x -> routeService.triggerRoute(x.getId()));
	//
	// routeService.getRoutesForUser(userId).stream().filter(x -> {
	// return x.getSource() != null && x.getSource().getIsActive();
	// })
	// .forEach(x -> routeService.triggerRoute(x.getId()));
	//
	// return Utils.httpOK(true);
	// }

	@RequestMapping(value = "/trigger", method = RequestMethod.POST)
	public ResponseEntity<Integer> triggerRoute(@RequestBody ObjectId id) {
		int numPostsPumped = routeService.triggerRoute(id);
		return Utils.httpOK(numPostsPumped);
	}

	@RequestMapping(value = "/triggerRoutes", method = RequestMethod.GET)
	public ResponseEntity<Integer> triggerRoutes(Principal currentUser) {
		String userId = currentUser.getName();
		int numPostsPumped = routeService.triggerRoute(userId);
		return Utils.httpOK(numPostsPumped);
	}

	@RequestMapping(value = "/facebook/feed", method = RequestMethod.POST)
	public ResponseEntity<Boolean> postUpdate() throws Exception {
		postService.updateAllRoutes();
		return Utils.httpOK(true);
	}

	@RequestMapping(value = "/facebook/feed/{routeId}", method = RequestMethod.POST)
	public ResponseEntity<Boolean> postUpdate(@PathVariable String routeId) throws Exception {
		Route route = routeService.findOne(new ObjectId(routeId));

		SocialConnection source = route.getSource();

		AbstractImporter processor = processorService.getImporter(source.getType());
		Collection<Post> posts = processor.process(source);
		postService.exportPost(posts, route.getTargets());

		return Utils.httpOK(true);
	}
}