package com.kdubb.pumpsocial.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.social.NotAuthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.exporter.Exporter;
import com.kdubb.pumpsocial.processor.AbstractImporter;
import com.kdubb.pumpsocial.processor.FacebookImporter;
import com.kdubb.pumpsocial.repository.safe.PostRepoSafe;
import com.kdubb.pumpsocial.repository.safe.RouteRepoSafe;
import com.kdubb.pumpsocial.repository.safe.SocialConnectionRepoSafe;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class PostService {

	@Inject
	private RouteRepoSafe routeRepo;

	@Inject
	private ProcessorService processorService;

	@Inject
	private FacebookImporter facebookImporter;

	@Inject
	private PostRepoSafe postRepo;

	@Inject
	private SocialConnectionRepoSafe socialRepo;

	@Inject
	private ExporterService exporterService;

	@Inject
	private ProblemService problemService;

	@Inject
	private CrawledPageService crawledPageService;

	private static final Logger LOG = LogManager.getLogger(PostService.class);

	@Scheduled(cron = "0 */2 * * * *")
	// 2 mins
	public void updateAllRoutes() {
		Collection<Route> routes;
		LOG.info("Checking routes");

		try {
			routes = routeRepo.findAll().stream().filter(x -> {
				return x.getSource() != null && x.getSource().getIsActive();
			}).collect(Collectors.toList());

			if (routes == null)
				return;

			LOG.info("routes.size()=" + routes.size());
			routes.parallelStream().forEach(x -> executeRoute(x));
			LOG.info("Done routes");
		} catch (DataAccessException e) {
			LOG.error("Failed to findAllRoutes", e);
			return;
		}
	}

	public void savePostIgnore(Post post) {
		post.setIsIgnore(true);
		postRepo.save(post);
	}

	public Collection<Post> findPosts(SocialConnection source) throws Exception {
		if (source.getType() == null) {
			LOG.error("source.getType() is null. Cannot find importer, skipping");
			return null;
		}

		AbstractImporter importer = processorService.getImporter(source.getType());

		if (importer == null) {
			LOG.error("No importer found for type [" + source.getType().toString() + "], skipping");
			return null;
		}

		try {
			Collection<Post> posts = importer.process(source);

			if (posts == null) {
				return null;
			}

			// Find posts on the source which either don't have a link or haven't been posted already
			posts = posts.stream().filter(x -> {
				if (StringUtils.isBlank(x.getLink()))
					return true;

				String linkWithoutProtocol = Utils.removeProtocol(x.getLink());
				List<Post> postsWithLink = postRepo.findByLinkLike(linkWithoutProtocol);
				return CollectionUtils.isEmpty(postsWithLink);
			}).collect(Collectors.toList());

			return posts;
		} catch (NotAuthorizedException e) {
			LOG.error("Failed to find posts on source socialConnection=" + source.getId(), e);
			problemService.authorizationProblem(source);
			throw e;
		}
	}

	private void delete(Route route) {
		if (route == null || route.getId() == null)
			return;

		SocialConnection source = route.getSource();

		if (source != null && SocialConnectionType.rss.equals(source.getType()))
			socialRepo.delete(source);

		for (SocialConnection connection : route.getTargets())
			if (connection != null && SocialConnectionType.rss.equals(connection.getType()))
				socialRepo.delete(connection);

		routeRepo.delete(route);
	}

	public int executeRoute(Route route) {
		SocialConnection source = route.getSource();

		if (!source.getIsActive()) {
			LOG.warn("Cannot execute inactive route : " + route.getId());
			return 0;
		}

		if (route.getTargets() == null) {
			LOG.warn("Cannot execute route with no targets : " + route.getId());
			return 0;
		}

		List<SocialConnection> activeTargets = route.getTargets()
				.stream()
				.filter(x -> x != null && x.getIsActive())
				.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(activeTargets)) {
			LOG.warn("Cannot execute route with no active targets : " + route.getId());
			return 0;
		}

		int count = 0;

		try {
			LOG.info("Going to check " + source.getType());
			Collection<Post> posts = findPosts(source);

			if (posts == null) {
				LOG.info("Found 0 posts for " + source.getType());
				return count;
			}

			// FB Facepalm
			if (source.getType() == SocialConnectionType.facebook) {
				posts.stream().forEach(x -> crawledPageService.setPageCrawled(x.getLink()));

				if (facebookImporter != null)
					facebookImporter.processAll(source).stream().forEach(x -> crawledPageService.setPageCrawled(x.getLink()));
			}

			count = posts.size();
			LOG.info("Found " + count + " posts for " + source.getType());

			exportPost(posts, activeTargets);
			LOG.info("Done exporting for " + source.getType());
		} catch (NotAuthorizedException e) {
			LOG.error("NotAuthorizedException encountered", e);
		} catch (Exception e) {
			LOG.error("Failed to process posts", e);
		}

		return count;
	}

	public void exportPost(Iterable<Post> posts, Collection<SocialConnection> targets) {
		if (posts == null) {
			LOG.warn("Nothing to post, posts null");
			return;
		}

		for (Post post : posts) {
			exportPostAllowed(post, targets);
		}
	}

	private void exportPostAllowed(Post post, Collection<SocialConnection> targets) {
		LOG.info("need to post: " + post.getLink());

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			LOG.error("Failed to sleep", e);
		}

		post.setScrapeTime(Instant.now().toEpochMilli());
		post.setTargets(targets);
		postRepo.save(post);
		LOG.info("Posting to " + targets.size() + " targets");

		for (SocialConnection socialConnection : targets) {
			if (socialConnection == null || socialConnection.getType() == null) {
				LOG.error("Either socialConnection is null or type is not set. Skipping");
				continue;
			}

			LOG.info("Posting to type " + socialConnection.getType());

			try {
				Exporter exporter = exporterService.getExporter(socialConnection.getType());
				exporter.export(post, socialConnection);
			} catch (NotAuthorizedException e) {
				LOG.error("Failed to post to socialConnection=" + socialConnection.getId(), e);
				problemService.authorizationProblem(socialConnection);
			} catch (Exception e) {
				LOG.error("Failed to export type [" + socialConnection.getType() + "]", e);
			}
		}
	}

	public long getPostCount(String userId) {
		return postRepo.countByUserId(userId);
	}

	public void deleteByUserId(String userId) {
		// TODO bring back
		postRepo.deleteByUserId(userId);
	}

	public Page<Post> findByUserId(String userId, Pageable page) {
		return postRepo.findByUserIdOrderByScrapeTimeDesc(userId, page);
	}
}
