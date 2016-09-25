package com.kdubb.pumpsocial.exporter;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.FacebookLink;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.enums.PostType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;

@Service
public class FacebookExporter extends Exporter {
	
	@Inject
	private OfflineConnectionRepository connectionRepo;
	
	private static final Logger LOG = LogManager.getLogger(FacebookExporter.class);
	
	@Override
	public void export(Post post, SocialConnection target) throws Exception {
		if(!SocialConnectionType.facebook.equals(target.getType())) {
			LOG.error("Cannot make a Facebook post to a Non-Facebook connection");
			return;
		}
		
		Facebook facebook = connectionRepo.getConnectionApi(target.getUserId(), Facebook.class);

		post.removeTag(target.getPumpTag());
		String message = postFromSocial(post);
		
		// Uploaded Video
		if(StringUtils.isNotBlank(post.getVideoUrl())) {
			try {
				Resource videoResource = new UrlResource(post.getVideoUrl());
				postVideo(facebook, target, videoResource, message);
			}
			catch (IOException e) {
				LOG.error("Failed to get Image", e);
			}
		}
		// Uploaded Image
		else if(StringUtils.isNotBlank(post.getImageUrl())) {
			try {
				Resource photoResource = new UrlResource(post.getImageUrl());
				
				// Manually uploaded G+ vid, add a link to the photo preview
				if(PostType.VIDEO.equals(post.getType())) {
					message += " " + post.getExternalLinks().get(0).getLink();
				}

				postPhoto(facebook, target, photoResource, message);
			}
			catch (IOException e) {
				LOG.error("Failed to get Image", e);
			}
		}
		// Link
		else if(CollectionUtils.isNotEmpty(post.getExternalLinks())) {
			ExternalLink externalLink = post.getExternalLinks().get(0);
			FacebookLink link = new FacebookLink(externalLink.getLink(), null, null, null);
			postLink(facebook, target, post.getContent(), link);
		}
		// Text
		else {
			postText(facebook, target, message);
		}
	}

	private void postText(Facebook facebook, SocialConnection target, String message) {
		if(target.getParent() == null)
			facebook.feedOperations().post(target.getTypeId(), message);
		else {
			facebook.pageOperations().post(target.getTypeId(), message);
		}
	}

	private void postLink(Facebook facebook, SocialConnection target, String message, FacebookLink link) {
		if(target.getParent() == null) {
			facebook.feedOperations().postLink(message, link);
		}
		else {
			facebook.pageOperations().post(target.getTypeId(), message, link);
		}
	}

	private void postVideo(Facebook facebook, SocialConnection target, Resource videoResource, String message) {
		if(target.getParent() == null)
			facebook.mediaOperations().postVideo(videoResource, null, message);
		else {
			facebook.pageOperations().postVideo(target.getTypeId(), videoResource, null, message);
		}
	}
	
	private void postPhoto(Facebook facebook, SocialConnection target, Resource photoResource, String message) {
		if(target.getParent() == null)
			facebook.mediaOperations().postPhoto(photoResource, message);
		else {
			facebook.pageOperations().postPhoto(target.getTypeId(), photoResource, message);
		}
	}

	private String postFromSocial(Post post) {
		return post.getContent();
	}

	private String postFromRss(Post post) {
		StringBuilder message = new StringBuilder();
		SocialConnection source = post.getSource();
		
		if(StringUtils.isNotBlank(source.getName()))
			message.append(source.getName());
		
		if(StringUtils.isNotBlank(post.getTitle())) {
			if(message.length() > 0)
				message.append(" - ");
			
			message.append(post.getTitle());
		}
		
		return message.toString();
	}
}