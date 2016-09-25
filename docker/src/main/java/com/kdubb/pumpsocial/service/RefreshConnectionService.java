package com.kdubb.pumpsocial.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.social.connect.mongodb.MongoConnection;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.FacebookProfile;
import org.springframework.social.facebook.api.GraphApi;
import org.springframework.social.facebook.api.Page;
import org.springframework.social.facebook.api.PagePicture;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.plus.Person;
import org.springframework.social.instagram.api.Instagram;
import org.springframework.social.instagram.api.InstagramProfile;
import org.springframework.social.tumblr.api.AvatarSize;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.social.tumblr.api.UserInfoBlog;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.TwitterProfile;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;

@Service
public class RefreshConnectionService {

	@Inject
	private SocialConnectionService socialConnectionService;

	@Inject
	private MongoConnectionDataService connectionService;

	@Inject
	private OfflineConnectionRepository offlineRepo;

	@Resource(name = "threadPoolTaskExecutor")
	private ThreadPoolTaskExecutor executor;

	private static final Logger LOG = LogManager.getLogger(RefreshConnectionService.class);

	public void refreshAll(String userId) {
		executor.execute(() -> {
			connectionService.findByUserId(userId).parallelStream().forEach(conn -> refresh(userId, conn));
		});
	}

	public void refresh(String userId, String id) {
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(id)) {
			return;
		}

		SocialConnection childConnection = socialConnectionService.findOne(id);

		if (childConnection == null)
			return;

		MongoConnection connection = childConnection.getConnection();
		refresh(userId, connection);
	}

	private void refresh(String userId, MongoConnection connection) {
		if (connection == null || !userId.equals(connection.getUserId())) {
			return;
		}

		switch (connection.getProviderId()) {
		case "tumblr":
			refreshTumblr(connection, userId);
			break;
		case "facebook":
			refreshFacebook(connection, userId);
			break;
		case "google":
			refreshGoogle(connection, userId);
			break;
		case "twitter":
			refreshTwitter(connection, userId);
			break;
		case "instagram":
			refreshInstagram(connection, userId);
			break;
		default:
			LOG.info("Cannot refresh unknown connection type [" + connection.getProviderId() + "]");
			return;
		}

		// save refreshed connection
		connectionService.save(connection);
	}

	private MongoConnection refreshTumblr(MongoConnection connection, String userId) {
		LOG.info("Refreshing Tumblr Connection");

		Tumblr tumblr = offlineRepo.getConnectionApi(userId, Tumblr.class);
		List<UserInfoBlog> blogs = tumblr.userOperations().info().getBlogs();
		Optional<UserInfoBlog> primaryBlog = blogs.stream().filter(x -> x.isPrimary()).findFirst();

		if (primaryBlog.isPresent()) {
			UserInfoBlog blogInfo = primaryBlog.get();
			connection.setDisplayName(blogInfo.getName());

			String imageUrl = getTumblrBlogImage(blogInfo, tumblr);
			connection.setImageUrl(imageUrl);
			connection.setProfileUrl(blogInfo.getUrl());
		}

		// Tumblr has pages, must check each individually
		Set<SocialConnection> socialConns = socialConnectionService.findByConnection(connection);

		Map<String, UserInfoBlog> pageIdMap = blogs.stream().collect(Collectors.toMap(x -> x.getUrl(), x -> x));

		Map<String, SocialConnection> socialConnectionMap = socialConns.stream().collect(Collectors.toMap(x -> x.getUrl(), x -> x));

		// Main Tumblr SocialConn
		Optional<SocialConnection> parentSocial = socialConns.stream()
				.filter(socialConnection -> connection.getProviderUserId().equals(socialConnection.getPageName()))
				.findFirst();

		// Tumblr pages
		socialConns.stream().forEach(socialConnection -> {
			UserInfoBlog blogInfo = pageIdMap.get(socialConnection.getUrl());

			// We no longer have access, but don't delete primary connection
			if (blogInfo == null && !socialConnection.getPageName().equals(connection.getProviderUserId())) {
				socialConnectionService.delete(socialConnection);
			}
			// Else update it
			else if (blogInfo != null) {
				socialConnection.setUrl(blogInfo.getUrl());

				String imageUrl = getTumblrBlogImage(blogInfo, tumblr);
				socialConnection.setImageUrl(imageUrl);
				socialConnection.setPageName(blogInfo.getName());
				socialConnection.setName(blogInfo.getTitle());
				socialConnectionService.save(socialConnection);
			}
		});

		// add blogs we didn't have before
		blogs.stream()
				.filter(blogInfo -> !blogInfo.isPrimary() && !socialConnectionMap.containsKey(blogInfo.getUrl()))
				.forEach(blogInfo -> {
					SocialConnection rootSocialConnection = parentSocial.get();

					String blogid = blogInfo.getUrl().replaceAll("http(s)?://", "");

					if (blogid.endsWith("/"))
						blogid = blogid.substring(0, blogid.length() - 1);

					SocialConnection pageConnection = new SocialConnection();
					pageConnection.setConnection(rootSocialConnection.getConnection());
					pageConnection.setUserId(rootSocialConnection.getUserId());
					pageConnection.setType(rootSocialConnection.getType());
					pageConnection.setTypeId(blogid);
					pageConnection.setUrl(blogInfo.getUrl());
					pageConnection.setPageName(blogInfo.getName());
					pageConnection.setName(blogInfo.getTitle());
					pageConnection.setIsSource(true);
					pageConnection.setIsTarget(true);
					pageConnection.setIsActive(true);

					String imageUrl = getTumblrBlogImage(blogInfo, tumblr);
					pageConnection.setImageUrl(imageUrl);

					pageConnection.setParent(rootSocialConnection);
					socialConnectionService.save(pageConnection);
				});

		return connection;
	}

	private MongoConnection refreshFacebook(MongoConnection connection, String userId) {
		LOG.info("Refreshing Facebook Connection");

		Facebook facebook = offlineRepo.getConnectionApi(userId, Facebook.class);
		FacebookProfile profile = facebook.userOperations().getUserProfile();

		connection.setDisplayName(profile.getName());

		String imageUrl = GraphApi.GRAPH_API_URL + profile.getId() + "/picture";
		connection.setImageUrl(imageUrl);

		String url = "http://facebook.com/profile.php?id=" + profile.getId();
		connection.setProfileUrl(url);

		// Facebook has pages, must check each individually
		Set<SocialConnection> socialConns = socialConnectionService.findByConnection(connection);

		// Main FB SocialConn
		Optional<SocialConnection> parentSocial = socialConns.stream()
				.filter(socialConnection -> socialConnection.getTypeId().equals(connection.getProviderUserId()))
				.findFirst();

		if (parentSocial.isPresent()) {
			SocialConnection socialConnection = parentSocial.get();
			socialConnection.setUrl(url);
			socialConnection.setImageUrl(imageUrl);
			socialConnection.setName(profile.getName());
			socialConnectionService.save(socialConnection);
		}

		PagedList<org.springframework.social.facebook.api.Account> facebookPages = facebook.pageOperations().getAccounts();

		Map<String, org.springframework.social.facebook.api.Account> pageIdMap = facebookPages.stream()
				.collect(Collectors.toMap(x -> x.getId(), x -> x));

		Map<String, SocialConnection> socialConnectionMap = socialConns.stream().collect(Collectors.toMap(x -> x.getTypeId(), x -> x));

		// FB pages
		socialConns.stream()
				.filter(socialConnection -> !socialConnection.getTypeId().equals(connection.getProviderUserId()))
				.forEach(socialConnection -> {
					org.springframework.social.facebook.api.Account account = pageIdMap.get(socialConnection.getTypeId());

					// We no longer have access
					if (account == null) {
						socialConnectionService.delete(socialConnection);
					}
					// Else update it
					else {
						Page page = facebook.pageOperations().getPage(socialConnection.getTypeId());

						socialConnection.setUrl(page.getLink());
						socialConnection.setImageUrl(getFacebookPageImage(facebook, account.getId()));
						socialConnection.setName(page.getName());
						socialConnection.setPermissions(account.getPermissions());
						socialConnectionService.save(socialConnection);
					}
				});

		// add accounts (pages) we didn't have before
		facebookPages.stream().filter(page -> !socialConnectionMap.containsKey(page.getId())).forEach(page -> {
			SocialConnection rootSocialConnection = parentSocial.get();

			SocialConnection pageConnection = new SocialConnection();
			pageConnection.setConnection(rootSocialConnection.getConnection());
			pageConnection.setParent(rootSocialConnection);
			pageConnection.setUserId(rootSocialConnection.getUserId());
			pageConnection.setType(rootSocialConnection.getType());
			pageConnection.setTypeId(page.getId());
			pageConnection.setUrl("https://www.facebook.com/pages/" + page.getName() + "/" + page.getId());
			pageConnection.setPageName(page.getName());
			pageConnection.setName(page.getName());
			pageConnection.setIsSource(true);
			pageConnection.setIsTarget(true);
			pageConnection.setIsActive(true);
			pageConnection.setPermissions(page.getPermissions());
			pageConnection.setImageUrl(getFacebookPageImage(facebook, page.getId()));

			pageConnection.setParent(rootSocialConnection);
			socialConnectionService.save(pageConnection);
		});

		return connection;
	}

	private String getFacebookPageImage(Facebook facebook, String pageId) {
		PagePicture pagePic = facebook.pageOperations().getPagePicture(pageId);

		if (pagePic == null)
			return null;

		return pagePic.getData().getUrl();
	}

	private String getTumblrBlogImage(UserInfoBlog blogInfo, Tumblr tumblr) {
		String blogId = getTumblrBlogId(blogInfo);
		return tumblr.blogOperations(blogId).avatar(AvatarSize.MEGA);
	}

	private String getTumblrBlogId(UserInfoBlog blogInfo) {
		String result = blogInfo.getUrl().replaceAll("http(s)?://", "");

		if (result.endsWith("/"))
			result = result.substring(0, result.length() - 1);

		return result;
	}

	private MongoConnection refreshGoogle(MongoConnection connection, String userId) {
		LOG.info("Refreshing Google Connection");

		Google google = offlineRepo.getConnectionApi(userId, Google.class);
		Person profile = google.plusOperations().getGoogleProfile();

		connection.setDisplayName(profile.getDisplayName());
		connection.setImageUrl(profile.getImageUrl());

		String url = "https://plus.google.com/" + profile.getId();
		connection.setProfileUrl(url);

		// Should just have the one for Google
		Set<SocialConnection> socialConns = socialConnectionService.findByConnection(connection);
		socialConns.forEach(socialConnection -> {
			socialConnection.setUrl(url);
			socialConnection.setImageUrl(profile.getImageUrl());
			socialConnection.setName(profile.getDisplayName());
			socialConnectionService.save(socialConnection);
		});

		return connection;
	}

	private MongoConnection refreshTwitter(MongoConnection connection, String userId) {
		LOG.info("Refreshing Twitter Connection");

		Twitter twitter = offlineRepo.getConnectionApi(userId, Twitter.class);
		TwitterProfile profile = twitter.userOperations().getUserProfile();

		connection.setDisplayName("@" + profile.getScreenName());
		connection.setImageUrl(profile.getProfileImageUrl());
		connection.setProfileUrl(profile.getProfileUrl());

		// Should just have the one for Twitter
		Set<SocialConnection> socialConns = socialConnectionService.findByConnection(connection);
		socialConns.forEach(socialConnection -> {
			socialConnection.setUrl(profile.getProfileUrl());
			socialConnection.setImageUrl(profile.getProfileImageUrl());
			socialConnection.setName(profile.getScreenName());
			socialConnectionService.save(socialConnection);
		});

		return connection;
	}

	private MongoConnection refreshInstagram(MongoConnection connection, String userId) {
		LOG.info("Refreshing Instagram Connection");

		Instagram insta = offlineRepo.getConnectionApi(userId, Instagram.class);
		InstagramProfile profile = insta.userOperations().getUser();

		connection.setDisplayName(profile.getUsername());
		connection.setImageUrl(profile.getProfilePictureUrl());

		String url = "http://instagram.com/" + profile.getUsername();
		connection.setProfileUrl(url);

		// Should just have the one for Insta
		Set<SocialConnection> socialConns = socialConnectionService.findByConnection(connection);
		socialConns.forEach(socialConnection -> {
			socialConnection.setUrl(url);
			socialConnection.setImageUrl(profile.getProfilePictureUrl());
			socialConnection.setName(profile.getFullName());
			socialConnectionService.save(socialConnection);
		});

		return connection;
	}
}
