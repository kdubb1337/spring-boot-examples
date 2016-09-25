package com.kdubb.pumpsocial.repository;

import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.domain.Route;
import com.kdubb.pumpsocial.domain.SocialConnection;

public interface RouteRepository extends MongoRepository<Route, ObjectId> {

	public Route findBySource(SocialConnection connection);
	
	public Set<Route> findByTargets(SocialConnection connection);
}