package com.kdubb.pumpsocial.processor;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.service.ProblemService;

public abstract class AbstractImporter {

	@Inject
	private ProblemService problemService;
	
	public abstract Collection<Post> process(SocialConnection source) throws Exception;
	
	protected void authorizationProblem(SocialConnection source) {
		problemService.authorizationProblem(source);
	}
	
	protected Post cleanPost(Post post) {
		if(post == null)
			return post;
		
		if(StringUtils.isNotBlank(post.getTitle())) {
			String title = normalizeCharacters(post.getTitle());
			post.setTitle(title);
		}
		
		if(StringUtils.isNotBlank(post.getContent())) {
			String content = normalizeCharacters(post.getContent());
			post.setContent(content);
		}
		
		return post;
	}
	
	private String normalizeCharacters(String str) {
		return str.replaceAll("’", "'")
				.replaceAll("‘", "'")
				.replaceAll("—", "-");
	}
}