package com.kdubb.pumpsocial.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.kdubb.pumpsocial.domain.Post;

public interface PostRepository extends MongoRepository<Post, String> {
	
	public List<Post> findByLinkLike(String link);
	
	public Long countByUserId(String userId);
	
//	@Query("delete from Post u where u.userId = ?1")
	public Long deleteByUserId(String userId);
	
	public Page<Post> findByUserIdOrderByScrapeTimeDesc(String userId, Pageable page);
}