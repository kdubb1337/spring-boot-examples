package com.kdubb.pumpsocial.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.service.MongoConnectionDataService;
import com.kdubb.pumpsocial.service.PostService;
import com.kdubb.pumpsocial.service.RefreshConnectionService;
import com.kdubb.pumpsocial.service.SocialConnectionService;
import com.kdubb.pumpsocial.util.Utils;

@RestController
@RequestMapping("/connection")
public class ConnectionController {
	
	@Inject
	private MongoConnectionDataService connectionService;
	
	@Inject
	private SocialConnectionService socialConnectionService;
	
	@Inject
	private RefreshConnectionService refreshConnectionService;
	
	@Inject
	private PostService postService;
	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public Map<String, Object> getConnections(Principal currentUser) {
		List<String> availableTypes = new ArrayList<String>();
		
		for(SocialConnectionType type : SocialConnectionType.values()) {
			availableTypes.add(type.toString());
		}
		
		availableTypes.remove("rss");
		
		Collection<SocialConnection> connections = socialConnectionService.findByUserId(currentUser.getName());
		Map<SocialConnectionType, SocialConnection> connectionMap = new HashMap<SocialConnectionType, SocialConnection>();
		
		for(SocialConnection connection : connections) {
			if(connection.getParent() == null) {
				connectionMap.put(connection.getType(), connection);
				availableTypes.remove(connection.getType().toString());
			}
		}
		
		for(SocialConnection connection : connections) {
			if(connection.getParent() == null)
				continue;
			
			SocialConnection parent = connectionMap.get(connection.getType());
			List<SocialConnection> pages = parent.getPages();
			
			if(pages == null)
				pages = new ArrayList<SocialConnection>();
			
			pages.add(connection);
			parent.setPages(pages);
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("connections", connectionMap.values());
		result.put("availableTypes", availableTypes);
		
//		LOG.info("connections --> " + Utils.toPrettyJson(result));
		
		return result;
	}
	
	@RequestMapping(value = "/social", method = RequestMethod.POST)
	public ResponseEntity<SocialConnection> updateSocialConnection(Principal currentUser, @RequestBody SocialConnection connection) {
		if(connection == null || connection.getId() == null || StringUtils.isBlank(connection.getId().toString()))
			Utils.httpError(null);
		
		SocialConnection saved = socialConnectionService.update(connection, currentUser.getName());
		return Utils.httpOK(saved);
	}
	
	@RequestMapping(value = "/updateTag", method = RequestMethod.POST)
	public ResponseEntity<Collection<Post>> updateSocialConnectionTag(Principal currentUser, @RequestBody SocialConnection connection) throws Exception {
		if(connection == null || connection.getId() == null || StringUtils.isBlank(connection.getId().toString()))
			Utils.httpError(null);
		
		SocialConnection merged = socialConnectionService.merge(connection, currentUser.getName());
		
		if(merged == null)
			return Utils.httpError(null);
		
		Collection<Post> posts = postService.findPosts(merged);
		
		// needs user confirmation if updating the tag would trigger posts
		if(!CollectionUtils.isEmpty(posts)) {
			return Utils.httpOK(posts);
		}
		
		socialConnectionService.save(merged);
		return Utils.httpOK(null);
	}
	
	@RequestMapping(value = "/updateTagWIngore", method = RequestMethod.POST)
	public ResponseEntity<Boolean> updateSocialConnectionTagWithIgnore(Principal currentUser, @RequestBody SocialConnection connection) throws Exception {
		if(connection == null || connection.getId() == null || StringUtils.isBlank(connection.getId().toString()))
			Utils.httpError(null);
		
		SocialConnection merged = socialConnectionService.merge(connection, currentUser.getName());
		
		if(merged == null)
			return Utils.httpError(null);
		
		
		// needs to ignore these posts so they aren't reposted after tag change
		postService.findPosts(merged).parallelStream()
			.forEach(x -> postService.savePostIgnore(x));
		
		socialConnectionService.save(merged);
		return Utils.httpOK(true);
	}	
	
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> deleteConnection(Principal currentUser, @PathVariable String name) {
		// TODO verify this
		connectionService.deleteByUserIdAndProviderId(currentUser.getName(), name);
		return Utils.httpOK(true);
	}
	
	@RequestMapping(value = "/primary/{type}", method = RequestMethod.GET)
	public ResponseEntity<Boolean> makeConnectionPrimary(Principal currentUser, @PathVariable String type) {
		socialConnectionService.makeConnectionPrimary(currentUser.getName(), type);
		return Utils.httpOK(true);
	}
	
	@RequestMapping(value = "/recheck/{id}", method = RequestMethod.GET)
	public Boolean updateConnection(Principal currentUser, @PathVariable String id) {
		refreshConnectionService.refresh(currentUser.getName(), id);
		return true;
	}
	
	@RequestMapping(value = "/refresh/google", method = RequestMethod.GET)
	public ResponseEntity<Boolean> refreshGoogle(Principal currentUser) {
		connectionService.findByUserId(currentUser.getName()).stream()
			.filter(conn -> StringUtils.isNotBlank(conn.getRefreshToken()))
			.forEach(conn -> {
				
			});
		
		return Utils.httpOK(true);
	}
}