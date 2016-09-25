package com.kdubb.pumpsocial.exporter;

import javax.inject.Inject;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.service.ProblemService;

public abstract class Exporter {

	@Inject
	private ProblemService problemService;
	
	public abstract void export(Post post, SocialConnection target) throws Exception;

	public void export(Post post, Iterable<SocialConnection> targets) throws Exception {
		for(SocialConnection target : targets)
			export(post, target);
	}
	
	public void export(Iterable<Post> posts, Iterable<SocialConnection> targets) throws Exception {
		for(Post post : posts)
			export(post, targets);
	}
	
	protected void authorizationProblem(SocialConnection source) {
		problemService.authorizationProblem(source);
	}
}