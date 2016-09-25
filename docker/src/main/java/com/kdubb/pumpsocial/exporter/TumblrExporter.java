package com.kdubb.pumpsocial.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.social.tumblr.api.BlogOperations;
import org.springframework.social.tumblr.api.ModifyLinkPost;
import org.springframework.social.tumblr.api.ModifyPhotoPost;
import org.springframework.social.tumblr.api.ModifyTextPost;
import org.springframework.social.tumblr.api.ModifyVideoPost;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.ExternalLink.LinkType;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.ClosureUtil;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class TumblrExporter extends Exporter {
	
	@Inject
	private OfflineConnectionRepository connectionRepo;
	
	private static final Logger LOG = LogManager.getLogger(FacebookExporter.class);
	
	@Override
	public void export(Post post, SocialConnection target) throws Exception {
		if(!SocialConnectionType.tumblr.equals(target.getType())) {
			LOG.error("Cannot make a Tumblr post to a Non-Tumblr connection");
			return;
		}
		
		Tumblr tumblr = connectionRepo.getConnectionApi(target.getUserId(), Tumblr.class);
		post.removeTag(target.getPumpTag());
		BlogOperations blogOps = tumblr.blogOperations(target.getTypeId());

		if(StringUtils.isNotBlank(target.getTypeId()) && StringUtils.isNotBlank(post.getLink())) {
			String message;
			
			if(SocialConnectionType.rss.equals(post.getSource().getType())) {
				message = postFromRss(post);
			}
			else {
				message = postFromSocial(post);
			}
			
			// UPLOADED VIDEO POST
			if(StringUtils.isNotBlank(post.getVideoUrl())) {
				ModifyVideoPost videoPost = new ModifyVideoPost();
				videoPost.setCaption(message);
				
				Resource videoResource = new UrlResource(post.getVideoUrl());
				videoPost.setData(videoResource);
				
				if(!CollectionUtils.isEmpty(post.getTags()))
					videoPost.setTags(post.getTags());
				
				blogOps.blogPostOperations().create(videoPost);
			}
			// PHOTO POST
			else if(StringUtils.isNotBlank(post.getImageUrl())) {
				try {
					Resource photoResource = new UrlResource(post.getImageUrl());
					
					ModifyPhotoPost photoPost = new ModifyPhotoPost();
					photoPost.setLink(post.getLink());
					photoPost.setCaption(message);
					
					if(!CollectionUtils.isEmpty(post.getTags()))
						photoPost.setTags(post.getTags());
					
					List<Resource> photos = new ArrayList<Resource>();
					photos.add(photoResource);
					photoPost.setData(photos);
					blogOps.blogPostOperations().create(photoPost);
				}
				catch (IOException e) {
					LOG.error("Failed to get Image", e);
				}
			}
			// LINK or EMBEDDED VIDEO
			else if(!CollectionUtils.isEmpty(post.getExternalLinks())) {
				ExternalLink externalLink = post.getExternalLinks().get(0);
				
				// EMBEDDED VIDEO
				if(LinkType.VIDEO.equals(externalLink.getType()) || Utils.isVideoLink(externalLink.getLink())) {
					ModifyVideoPost videoPost = new ModifyVideoPost();
					videoPost.setCaption(message);
					videoPost.setEmbed(Utils.getEmbed(externalLink.getLink()));
					
					if(!CollectionUtils.isEmpty(post.getTags()))
						videoPost.setTags(post.getTags());
					
					blogOps.blogPostOperations().create(videoPost);
				}
				// LINK POST
				else {
					ModifyLinkPost linkPost = new ModifyLinkPost();
					linkPost.setDescription(message);
					linkPost.setUrl(externalLink.getLink());
					
					if(!CollectionUtils.isEmpty(post.getTags()))
						linkPost.setTags(post.getTags());
					
					blogOps.blogPostOperations().create(linkPost);
				}
			}
			// TEXT POST
			else {
				ModifyTextPost textPost = new ModifyTextPost();
				textPost.setBody(message);
				
				if(!CollectionUtils.isEmpty(post.getTags()))
					textPost.setTags(post.getTags());
				
				blogOps.blogPostOperations().create(textPost);
			}
		}
	}

	private String postFromSocial(Post post) {
		StringBuilder message = new StringBuilder();
		String content = Utils.maxLength(post.getContent(), 500);
		message.append(content);
		
		if(post.getContent() != null && post.getContent().length() > 500)
			message.append(" <a href=\"")
				.append(post.getLink())
				.append("\" target=\"_blank\">Read more</a>");
		
		return message.toString();
	}

	private String postFromRss(Post post) {
		StringBuilder message = new StringBuilder();
		message.append("<b><a href=\"")
			.append(post.getLink())
			.append("\" target=\"_blank\">");
		
		SocialConnection source = post.getSource();
		
		if(StringUtils.isNotBlank(source.getName()))
			message.append(source.getName());
		
		if(StringUtils.isNotBlank(post.getTitle())) {
			if(message.length() > 0)
				message.append(" - ");
			
			message.append(post.getTitle());
		}
		
		message.append("</b></a><br/><br/>");
		String content = Utils.maxLength(post.getContent(), 200);
		message.append(content)
			.append(" <a href=\"")
			.append(post.getLink())
			.append("\" target=\"_blank\">Read more</a>");
		
		return message.toString();
	}
}