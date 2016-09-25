package com.kdubb.pumpsocial.controller;

import java.security.Principal;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.service.PostService;
import com.kdubb.pumpsocial.util.Utils;

@RestController
@RequestMapping("/post")
public class PostController {

	@Inject
	private PostService postService;
	
	private static final Logger LOG = LogManager.getLogger(PostController.class);
	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<Page<Post>> getPosts(@RequestParam int page, @RequestParam int pageSize, Principal currentUser) {
		return Utils.httpOK(postService.findByUserId(currentUser.getName(), new PageRequest(page, pageSize)));
	}
	
	@ResponseBody
	@ExceptionHandler(HttpSessionRequiredException.class)
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason="The session has expired")
	public String handleSessionExpired() {
		LOG.info("sessionExpired!!");
		return "sessionExpired";
	}
}