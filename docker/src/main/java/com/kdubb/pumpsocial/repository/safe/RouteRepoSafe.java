package com.kdubb.pumpsocial.repository.safe;

import java.util.Set;

import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.repository.RouteRepository;

@Service
public class RouteRepoSafe extends AbstractRepeaterRepo<Route, ObjectId> implements RouteRepository {

	@Inject
	private RouteRepository routeRepo;

	@Override
	protected MongoRepository<Route, ObjectId> getRepo() {
		return routeRepo;
	}

	@Override
	public Set<Route> findByTargets(final SocialConnection connection) {
		return repeaterService.retryIfNecessary(() -> routeRepo.findByTargets(connection));
	}

	// @Override
	// public Set<Route> findByIsActiveTrue() {
	// return repeaterService.retryIfNecessary(() -> routeRepo.findByIsActiveTrue());
	// }

	@Override
	public Route findBySource(final SocialConnection connection) {
		return repeaterService.retryIfNecessary(() -> routeRepo.findBySource(connection));
	}
}