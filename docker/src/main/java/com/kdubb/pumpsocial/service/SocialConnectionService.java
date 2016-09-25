package com.kdubb.pumpsocial.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.social.connect.mongodb.MongoConnection;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.FacebookProfile;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PageOperations;
import org.springframework.social.facebook.api.PagePicture;
import org.springframework.social.google.api.Google;
import org.springframework.social.instagram.api.Instagram;
import org.springframework.social.tumblr.api.AvatarSize;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.social.tumblr.api.UserInfoBlog;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.domain.response.AppState;
import com.kdubb.pumpsocial.repository.safe.RouteRepoSafe;
import com.kdubb.pumpsocial.repository.safe.SocialConnectionRepoSafe;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class SocialConnectionService {

	@Inject
	private ApiService apiService;

	@Inject
	private RouteService routeService;

	@Inject
	private RouteRepoSafe routeRepo;

	@Inject
	private SocialConnectionRepoSafe socialConnectionRepo;

	private static final Logger LOG = LogManager.getLogger(SocialConnectionService.class);

	// TODO this is expensive, should be saved on creation instead
	public String getSocialConnectionName(SocialConnection socialConnection) {
		if (socialConnection == null)
			return "<null>";

		if (StringUtils.isNotBlank(socialConnection.getName()))
			return socialConnection.getName();

		LOG.info("getSocialConnectionName:" + Utils.toPrettyJson(socialConnection));

		switch (socialConnection.getType()) {
			case facebook:
				Facebook facebook = apiService.getFacebook();

				if (StringUtils.isBlank(socialConnection.getTypeId())) {
					FacebookProfile profile = facebook.userOperations().getUserProfile();
					return profile.getLastName() + ", " + profile.getFirstName();
				}

				Page page = facebook.pageOperations().getPage(socialConnection.getTypeId());
				return page.getName();
			case twitter:
				Twitter twitter = apiService.getTwitter();
				return twitter.userOperations().getScreenName();
			case tumblr:
				Tumblr tumblr = apiService.getTumblr();
				return "Tumblr-" + tumblr.blogOperations(socialConnection.getTypeId()).info().getTitle();
			case google:
				Google google = apiService.getGoogle();
				return google.userOperations().getUserInfo().getLastName() + ", " + google.userOperations().getUserInfo().getFirstName();
			case instagram:
				Instagram instagram = apiService.getInstagram();
				return instagram.userOperations().getUser().getFullName();
			case rss:
				return socialConnection.getName();
			default:
				return "unknown type";
		}
	}

	public void save(Iterable<SocialConnection> connections) {
		for (SocialConnection connection : connections)
			save(connection);
	}

	public SocialConnection save(SocialConnection connection) {
		SocialConnection result;

		if (connection.getId() == null) {
			result = socialConnectionRepo.save(connection);
		} else {
			SocialConnection existing = socialConnectionRepo.findOne(connection.getId());

			if (existing == null) {
				LOG.warn("Failed to find SocialConnection by id=[" + connection.getId().toString() + "]");
				result = socialConnectionRepo.save(connection);
				return result;
			}

			connection.merge(existing);
			result = socialConnectionRepo.save(existing);
		}

		return result;
	}

	public SocialConnection update(SocialConnection connection, String userId) {
		SocialConnection merged = merge(connection, userId);
		save(merged);
		return merged;
	}

	public SocialConnection merge(SocialConnection connection, String userId) {
		if (connection.getId() == null)
			return null;

		SocialConnection existing = socialConnectionRepo.findOne(connection.getId());

		// if the connection doesn't exist or it belongs to someone else
		if (existing == null || !existing.getUserId().equals(userId))
			return null;

		connection.merge(existing);
		return existing;
	}

	public Set<SocialConnection> findByConnection(MongoConnection connection) {
		return socialConnectionRepo.findByConnection(connection);
	}

	public List<SocialConnection> createRelatedConnections(MongoConnection mongoConnection) {
		SocialConnection socialConnection = new SocialConnection();
		socialConnection.setConnection(mongoConnection);
		socialConnection.setUserId(mongoConnection.getUserId());
		socialConnection.setType(SocialConnectionType.valueOf(mongoConnection.getProviderId()));
		socialConnection.setTypeId(mongoConnection.getProviderUserId());
		socialConnection.setUrl(mongoConnection.getProfileUrl());
		socialConnection.setImageUrl(mongoConnection.getImageUrl());
		socialConnection.setIsActive(true);
		socialConnection.setEmailCount(0);
		socialConnection.setLastEmailMills(0L);

		if (!SocialConnectionType.tumblr.equals(socialConnection.getType())) {
			String name = getSocialConnectionName(socialConnection);
			socialConnection.setName(name);
		}

		setConnectionAvailability(socialConnection);

		LOG.info("mongoConnection.getUserId() --> " + mongoConnection.getUserId());
		List<SocialConnection> conns = socialConnectionRepo.findByUserId(mongoConnection.getUserId());
		// LOG.info("conns --> " + Utils.toPrettyJson(conns));

		Optional<SocialConnection> existingConn = conns.stream().filter(x -> {
			return x.equals(socialConnection);
		}).findFirst();

		LOG.info("existingConn --> " + Utils.toPrettyJson(existingConn));

		List<SocialConnection> result = new ArrayList<>();

		// If it already exists, it was disconnected and we don't want to create page connections again
		if (existingConn.isPresent()) {
			SocialConnection existing = existingConn.get();
			existing.setIsActive(true);
			existing.setEmailCount(0);
			existing.setConnection(mongoConnection);
			result.add(socialConnectionRepo.save(existing));

			conns.stream().filter(x -> existing.equals(x.getParent())).forEach(x -> {
				x.setIsActive(true);
				x.setEmailCount(0);
				socialConnectionRepo.save(x);
			});
		} else {
			SocialConnection newConnection = socialConnectionRepo.save(socialConnection);
			result.add(newConnection);

			List<SocialConnection> pageConnections = createConnectionsForPages(newConnection);
			result.addAll(pageConnections);
		}

		return result;
	}

	private List<SocialConnection> createConnectionsForPages(SocialConnection socialConnection) {
		List<SocialConnection> result = new ArrayList<>();

		switch (socialConnection.getType()) {
			case facebook:
				PageOperations pageOps = apiService.getFacebook().pageOperations();
				for (org.springframework.social.facebook.api.Account account : pageOps.getAccounts()) {
					// LOG.info("account.getId()=" + account.getId() + " --> " + socialConnection.get);
					SocialConnection pageConnection = new SocialConnection();
					pageConnection.setConnection(socialConnection.getConnection());
					pageConnection.setParent(socialConnection);
					pageConnection.setUserId(socialConnection.getUserId());
					pageConnection.setType(socialConnection.getType());
					pageConnection.setTypeId(account.getId());
					pageConnection.setUrl("https://www.facebook.com/pages/" + account.getName() + "/" + account.getId());
					pageConnection.setPageName(account.getName());
					pageConnection.setName(account.getName());
					pageConnection.setIsSource(true);
					pageConnection.setIsTarget(true);
					pageConnection.setIsActive(true);
					pageConnection.setPermissions(account.getPermissions());

					PagePicture pagePic = pageOps.getPagePicture(account.getId());

					if (pagePic != null) {
						pageConnection.setImageUrl(pagePic.getData().getUrl());
					}

					SocialConnection savedConnection = socialConnectionRepo.save(pageConnection);
					result.add(savedConnection);
				}

				break;
			case tumblr:
				for (UserInfoBlog blogInfo : apiService.getTumblr().userOperations().info().getBlogs()) {
					String blogid = blogInfo.getUrl().replaceAll("http(s)?://", "");

					if (blogid.endsWith("/"))
						blogid = blogid.substring(0, blogid.length() - 1);

					SocialConnection pageConnection = new SocialConnection();
					pageConnection.setConnection(socialConnection.getConnection());
					pageConnection.setUserId(socialConnection.getUserId());
					pageConnection.setType(socialConnection.getType());
					pageConnection.setTypeId(blogid);
					pageConnection.setUrl(blogInfo.getUrl());
					pageConnection.setPageName(blogInfo.getName());
					pageConnection.setName(blogInfo.getTitle());
					pageConnection.setIsSource(true);
					pageConnection.setIsTarget(true);
					pageConnection.setIsActive(true);

					if (blogInfo.isPrimary()) {
						pageConnection.setId(socialConnection.getId());
						pageConnection.merge(socialConnection);
						socialConnectionRepo.save(socialConnection);
					} else {
						String imageUrl = apiService.getTumblr().blogOperations(blogid).avatar(AvatarSize.MEGA);
						pageConnection.setImageUrl(imageUrl);

						pageConnection.setParent(socialConnection);

						SocialConnection savedConnection = socialConnectionRepo.save(pageConnection);
						result.add(savedConnection);
					}
				}
				break;
			default:
				break;
		}

		return result;
	}

	private void setConnectionAvailability(SocialConnection socialConnection) {
		switch (socialConnection.getType()) {
			case facebook:
				socialConnection.setIsSource(false);
				socialConnection.setIsTarget(true);
				break;
			case google:
				socialConnection.setIsSource(true);
				socialConnection.setIsTarget(false);
				break;
			case instagram:
				socialConnection.setIsSource(true);
				socialConnection.setIsTarget(false);
				break;
			case rss:
				socialConnection.setIsSource(true);
				socialConnection.setIsTarget(false);
				break;
			case tumblr:
				socialConnection.setIsSource(false);
				socialConnection.setIsTarget(false);
				break;
			case twitter:
				socialConnection.setIsSource(true);
				socialConnection.setIsTarget(true);
				break;
			default:
				break;
		}
	}

	public AppState getAppState(String userId) {
		AppState state = new AppState();

		LOG.info("start getAppState()");
		List<SocialConnection> connections = findByUserId(userId);
		state.setConnectedNetworks(connections);

		Collection<Route> routes = routeService.getRoutesForUser(userId);
		state.setRoutes(routes);
		LOG.info("end getAppState()");

		return state;
	}

	public List<SocialConnection> findByUserId(String userId) {
		return socialConnectionRepo.findByUserId(userId);
	}

	public Route findBySource(SocialConnection socialConnection) {
		return routeRepo.findBySource(socialConnection);
	}

	public SocialConnection findOne(String id) {
		return socialConnectionRepo.findOne(new ObjectId(id));
	}

	public SocialConnection findOne(ObjectId id) {
		return socialConnectionRepo.findOne(id);
	}

	public void addToRoute(SocialConnection source, SocialConnection target) {
		Route route = findBySource(source);

		if (route == null) {
			route = new Route();
			route.setSource(source);
		}

		Set<SocialConnection> targets = route.getTargets();

		if (targets == null)
			targets = new HashSet<SocialConnection>();

		targets.add(target);
		route.setTargets(targets);
		saveRoute(route);
	}

	public void saveRoute(Route route) {
		if (route.getId() == null) {
			routeRepo.save(route);
		} else {
			Route existing = routeRepo.findOne(route.getId());

			if (existing == null) {
				LOG.warn("Failed to find Route by id=[" + route.getId().toString() + "]");
				routeRepo.save(route);
				return;
			}

			route.merge(existing);
			routeRepo.save(existing);
		}
	}

	public void makeConnectionPrimary(String userId, String type) {
		List<SocialConnection> connections = findByUserId(userId);

		// Set old primary to false
		connections.stream().filter(x -> x.getIsPrimary() != null && x.getIsPrimary()).forEach(x -> {
			x.setIsPrimary(false);
			save(x);
		});

		// Set new primary
		connections.stream()
				// Is it the root connection of the right type?
				.filter(x -> x.getParent() == null && type.equals(x.getType().toString())).forEach(x -> {
					x.setIsPrimary(true);
					save(x);
				});
	}

	public void deleteByMongoConnection(MongoConnection connection) {
		Set<SocialConnection> socialConnections = socialConnectionRepo.findByConnection(connection);
		socialConnections.forEach(x -> deleteRouteBySocialConnection(x));
		socialConnectionRepo.delete(socialConnections);
	}

	// Only use when you know what you're doing
	public void delete(SocialConnection socialConnection) {
		deleteRouteBySocialConnection(socialConnection);
		socialConnectionRepo.delete(socialConnection);
	}

	public void deleteRouteBySocialConnection(SocialConnection socialConnection) {
		Route sourceRoute = findBySource(socialConnection);

		if (sourceRoute != null)
			routeRepo.delete(sourceRoute);

		Set<Route> targetRoutes = routeRepo.findByTargets(socialConnection);

		if (!CollectionUtils.isEmpty(targetRoutes)) {
			for (Route targetRoute : targetRoutes) {
				if (targetRoute.getTargets().size() == 1)
					routeRepo.delete(targetRoute);
				else {
					SocialConnection toRemove = null;
					Set<SocialConnection> targets = targetRoute.getTargets();

					for (SocialConnection targetConn : targets) {
						if (targetConn != null && targetConn.getId().equals(socialConnection.getId())) {
							toRemove = targetConn;
							break;
						}
					}

					LOG.info("toRemove --> " + Utils.toPrettyJson(toRemove));
					targets.remove(toRemove);
					targetRoute.setTargets(targets);

					// TODO replace this
					// routeRepo.updateRoute(targetRoute, socialConnection.getUserId());
				}
			}
		}
	}

	public void setActive(MongoConnection mongoConnection) {
		this.findByConnection(mongoConnection).stream().filter(x -> {
			return !x.getIsActive();
		}).forEach(conn -> {
			conn.setIsActive(true);
			conn.setEmailCount(0);
			conn.setLastEmailMills(0L);
			this.update(conn, mongoConnection.getUserId());
		});
	}
}