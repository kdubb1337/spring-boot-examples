package com.kdubb.pumpsocial.repository;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.social.connect.mongodb.MongoConnection;

import com.kdubb.pumpsocial.domain.SocialConnection;

public interface SocialConnectionRepository extends MongoRepository<SocialConnection, ObjectId> {

	public List<SocialConnection> findByUserId(String userId);
	
	public Page<SocialConnection> findByUserId(String userId, Pageable pageable);
	
	public Set<SocialConnection> findByConnection(MongoConnection connection);
	
	public Set<SocialConnection> findByUserIdAndParentIsNull(String userId);
}