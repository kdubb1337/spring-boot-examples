/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.connect.mongodb;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.tumblr.api.AvatarSize;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.kdubb.pumpsocial.domain.RefreshToken;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.service.MongoConnectionDataService;
import com.kdubb.pumpsocial.service.RefreshTokenService;
import com.kdubb.pumpsocial.service.SocialConnectionService;
import com.kdubb.pumpsocial.service.WebsocketService;
import com.mongodb.WriteConcern;

/**
 * A service for the spring connections management using Mongodb.
 *
 * @author Carlo P. Micieli
 */
@Service
public class MongoConnectionService implements ConnectionService {

	@Inject
	private SocialConnectionService socialConnectionService;

	@Inject
	private MongoConnectionDataService mongoConnectionDataService;

	@Inject
	private RefreshTokenService refreshTokenService;

	@Autowired
	private WebsocketService websocketService;

	private final MongoTemplate mongoTemplate;
	private final ConnectionConverter converter;

	private static final Logger LOG = LogManager.getLogger(MongoConnectionService.class);

	@Autowired
	public MongoConnectionService(MongoTemplate mongoTemplate, ConnectionConverter converter) {
		this.mongoTemplate = mongoTemplate;
		this.converter = converter;
	}

	/**
	 * Returns the max connection rank for the user and the provider.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getMaxRank(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public int getMaxRank(String userId, String providerId) {
		// select coalesce(max(rank) + 1, 1) as rank from UserConnection where userId = ? and providerId = ?
		Query query = query(where("userId").is(userId).and("providerId").is(providerId));

		Sort.Order order = new Sort.Order(Direction.DESC, "rank");
		query.with(new Sort(order));

		MongoConnection cnn = mongoTemplate.findOne(query, MongoConnection.class);

		if (cnn == null)
			return 1;

		return cnn.getRank() + 1;
	}

	/**
	 * Create a new connection for the user.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#create(java.lang.String,
	 *      org.springframework.social.connect.Connection, int)
	 */
	@Override
	public void create(String userId, Connection<?> userConn, int rank) {
		MongoConnection mongoConnection = converter.convert(userConn);
		mongoConnection.setUserId(userId);
		mongoConnection.setRank(rank);

		if (StringUtils.isBlank(mongoConnection.getImageUrl())) {
			Object api = userConn.getApi();

			if (Tumblr.class.isAssignableFrom(api.getClass())) {
				Tumblr tumblr = (Tumblr) api;

				String imageUrl = tumblr.blogOperations(userConn.getDisplayName() + ".tumblr.com").avatar(AvatarSize.MEGA);
				mongoConnection.setImageUrl(imageUrl);
			}
		}

		List<SocialConnection> socialConns = null;

		try {
			if (mongoConnection.getProviderId().equalsIgnoreCase("facebook") || mongoConnection.getProviderId().equalsIgnoreCase("google")) {
				MongoConnection existsUnderDifferentUser = mongoConnectionDataService.findByProviderIdAndProviderUserId(mongoConnection.getProviderId(),
						mongoConnection.getProviderUserId());

				if (existsUnderDifferentUser != null && existsUnderDifferentUser.getUserId() != userId) {
					LOG.warn("Cannot add a " + mongoConnection.getProviderId() + " account under two different accounts because it causes issues during login");
					throw new RuntimeException();
				}
			}

			MongoConnection existing = mongoConnectionDataService.findByUserIdAndProviderIdAndProviderUserId(userId, mongoConnection.getProviderId(),
					mongoConnection.getProviderUserId());

			if (existing == null) {
				setRefreshToken(mongoConnection);

				mongoTemplate.insert(mongoConnection);
				socialConns = socialConnectionService.createRelatedConnections(mongoConnection);
			} else {
				// Merge
				existing.setAccessToken(mongoConnection.getAccessToken());
				existing.setSecret(mongoConnection.getSecret());

				setRefreshToken(mongoConnection);

				if (StringUtils.isNotBlank(mongoConnection.getRefreshToken()))
					existing.setRefreshToken(mongoConnection.getRefreshToken());

				existing.setExpireTime(mongoConnection.getExpireTime());

				mongoConnectionDataService.save(existing);
				socialConns = socialConnectionService.createRelatedConnections(existing);
			}
		}
		catch (Exception e) {
			LOG.warn("Couldn't create mongoConnection: " + e.getMessage(), e);

			Optional<MongoConnection> existing = mongoConnectionDataService.findByUserId(userId).stream().filter(x -> {
				return mongoConnection.equals(x);
			}).findFirst();

			if (existing.isPresent())
				socialConnectionService.setActive(existing.get());
		}

		websocketService.notify(userId, "CONNECTION_ADDED", socialConns);
	}

	private void setRefreshToken(MongoConnection mongoConnection) {
		RefreshToken existingToken = refreshTokenService.findByProviderIdAndProviderUserId(mongoConnection.getProviderId(),
				mongoConnection.getProviderUserId());

		if (existingToken != null)
			LOG.info("existingToken.getRefreshToken() --> " + existingToken.getRefreshToken());

		LOG.info("mongoConnection.getRefreshToken() --> " + mongoConnection.getRefreshToken());
		LOG.info("StringUtils.isNotBlank(mongoConnection.getRefreshToken()) --> " + StringUtils.isNotBlank(mongoConnection.getRefreshToken()));

		// We have a saved refresh token, but it needs to be attached to current connection
		if (StringUtils.isBlank(mongoConnection.getRefreshToken()) && existingToken != null) {
			mongoConnection.setRefreshToken(existingToken.getRefreshToken());
		}
		// We haven't saved the refresh token before
		else if (StringUtils.isNotBlank(mongoConnection.getRefreshToken()) && existingToken == null) {
			RefreshToken refresh = new RefreshToken();
			refresh.setProviderId(mongoConnection.getProviderId());
			refresh.setProviderUserId(mongoConnection.getProviderUserId());
			refresh.setRefreshToken(mongoConnection.getRefreshToken());
			refreshTokenService.save(refresh);
		}
		// Refresh token changed
		else if (StringUtils.isNotBlank(mongoConnection.getRefreshToken()) && !mongoConnection.getRefreshToken().equals(existingToken.getRefreshToken())) {
			existingToken.setRefreshToken(mongoConnection.getRefreshToken());
			refreshTokenService.save(existingToken);
		}
	}

	/**
	 * Update a connection.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#update(java.lang.String,
	 *      org.springframework.social.connect.Connection)
	 */
	@Override
	public void update(String userId, Connection<?> userConn) {
		MongoConnection mongoCnn = converter.convert(userConn);
		mongoCnn.setUserId(userId);

		try {
			mongoTemplate.setWriteConcern(WriteConcern.SAFE);
			mongoTemplate.save(mongoCnn);
		}
		catch (DuplicateKeyException e) {
			findAndModify(userId, mongoCnn);
		}
	}

	private void findAndModify(String userId, MongoConnection mongoConnection) {
		Query q = query(
				where("userId").is(userId).and("providerId").is(mongoConnection.getProviderId()).and("providerUserId").is(mongoConnection.getProviderUserId()));

		Update update = Update.update("expireTime", mongoConnection.getExpireTime()).set("accessToken", mongoConnection.getAccessToken())
				.set("profileUrl", mongoConnection.getProfileUrl()).set("imageUrl", mongoConnection.getImageUrl())
				.set("displayName", mongoConnection.getDisplayName());

		mongoTemplate.findAndModify(q, update, MongoConnection.class);
	}

	/**
	 * Remove a connection.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#remove(java.lang.String,
	 *      org.springframework.social.connect.ConnectionKey)
	 */
	@Override
	public void remove(String userId, ConnectionKey connectionKey) {
		LOG.info("remove userId=" + userId);

		// delete where userId = ? and providerId = ? and providerUserId = ?
		Query q = query(
				where("userId").is(userId).and("providerId").is(connectionKey.getProviderId()).and("providerUserId").is(connectionKey.getProviderUserId()));
		mongoTemplate.remove(q, MongoConnection.class);
	}

	/**
	 * Remove all the connections for a user on a provider.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#remove(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void remove(String userId, String providerId) {
		LOG.info("remove userId=" + userId + " | providerId=" + providerId);

		// delete where userId = ? and providerId = ?
		Query q = query(where("userId").is(userId).and("providerId").is(providerId));

		mongoTemplate.remove(q, MongoConnection.class);
	}

	/**
	 * Return the primary connection.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getPrimaryConnection(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public Connection<?> getPrimaryConnection(String userId, String providerId) {
		// where userId = ? and providerId = ? and rank = 1
		Query q = query(where("userId").is(userId).and("providerId").is(providerId).and("rank").is(1));

		MongoConnection mc = mongoTemplate.findOne(q, MongoConnection.class);
		return converter.convert(mc);
	}

	/**
	 * Get the connection for user, provider and provider user id.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getConnection(java.lang.String,
	 *      java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public Connection<?> getConnection(String userId, String providerId, String providerUserId) {
		// where userId = ? and providerId = ? and providerUserId = ?
		Query q = query(where("userId").is(userId).and("providerId").is(providerId).and("providerUserId").is(providerUserId));

		MongoConnection mc = mongoTemplate.findOne(q, MongoConnection.class);
		return converter.convert(mc);
	}

	/**
	 * Get all the connections for an user id.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getConnections(java.lang.String)
	 */
	@Override
	public List<Connection<?>> getConnections(String userId) {
		// select where userId = ? order by providerId, rank
		Query query = query(where("userId").is(userId));

		Sort.Order providerOrder = new Sort.Order(Direction.ASC, "providerId");
		Sort.Order rankOrder = new Sort.Order(Direction.ASC, "rank");
		query.with(new Sort(providerOrder, rankOrder));

		return runQuery(query);
	}

	/**
	 * Get all the connections for an user id on a provider.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getConnections(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public List<Connection<?>> getConnections(String userId, String providerId) {
		// where userId = ? and providerId = ? order by rank
		Query query = new Query(where("userId").is(userId).and("providerId").is(providerId));

		Sort.Order rankOrder = new Sort.Order(Direction.ASC, "rank");
		query.with(new Sort(rankOrder));

		return runQuery(query);
	}

	/**
	 * Get all the connections for an user.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getConnections(java.lang.String,
	 *      org.springframework.util.MultiValueMap)
	 */
	@Override
	public List<Connection<?>> getConnections(String userId, MultiValueMap<String, String> providerUsers) {
		// userId? and providerId = ? and providerUserId in (?, ?, ...) order by providerId, rank

		if (providerUsers == null || providerUsers.isEmpty()) {
			throw new IllegalArgumentException("Unable to execute find: no providerUsers provided");
		}

		List<Criteria> lc = new ArrayList<Criteria>();
		for (Entry<String, List<String>> entry : providerUsers.entrySet()) {
			String providerId = entry.getKey();

			lc.add(where("providerId").is(providerId).and("providerUserId").in(entry.getValue()));
		}

		Criteria criteria = where("userId").is(userId);
		criteria.orOperator(lc.toArray(new Criteria[lc.size()]));

		Query query = new Query(criteria);

		Sort.Order providerOrder = new Sort.Order(Direction.ASC, "providerId");
		Sort.Order rankOrder = new Sort.Order(Direction.ASC, "rank");
		query.with(new Sort(providerOrder, rankOrder));

		return runQuery(query);
	}

	/**
	 * Get the user ids on the provider.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getUserIds(java.lang.String,
	 *      java.util.Set)
	 */
	@Override
	public Set<String> getUserIds(String providerId, Set<String> providerUserIds) {
		// select userId from " + tablePrefix + "UserConnection where providerId = :providerId and providerUserId in
		// (:providerUserIds)
		Query q = query(where("providerId").is(providerId).and("providerUserId").in(new ArrayList<String>(providerUserIds)));
		q.fields().include("userId");

		List<MongoConnection> results = mongoTemplate.find(q, MongoConnection.class);
		Set<String> userIds = new HashSet<String>();
		for (MongoConnection mc : results) {
			userIds.add(mc.getUserId());
		}

		return userIds;
	}

	/**
	 * Get the user ids on the provider with a given provider user id.
	 * 
	 * @see org.springframework.social.connect.UserInfoService.ConnectionService#getUserIds(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public List<String> getUserIds(String providerId, String providerUserId) {
		// select userId where providerId = ? and providerUserId = ?",
		Query q = query(where("providerId").is(providerId).and("providerUserId").is(providerUserId));
		q.fields().include("userId");

		List<MongoConnection> results = mongoTemplate.find(q, MongoConnection.class);
		List<String> userIds = new ArrayList<String>();
		for (MongoConnection mc : results) {
			userIds.add(mc.getUserId());
		}

		return userIds;
	}

	// helper methods

	private List<Connection<?>> runQuery(Query query) {
		List<MongoConnection> results = mongoTemplate.find(query, MongoConnection.class);
		List<Connection<?>> l = new ArrayList<Connection<?>>();
		for (MongoConnection mc : results) {
			l.add(converter.convert(mc));
		}

		return l;
	}
}