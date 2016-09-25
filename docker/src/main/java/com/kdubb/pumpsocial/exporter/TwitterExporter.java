package com.kdubb.pumpsocial.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.social.connect.Connection;
import org.springframework.social.twitter.api.TweetData;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.ClosureUtil;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class TwitterExporter extends Exporter {

	@Inject
	private OfflineConnectionRepository connectionRepo;
	
	private static final int LINK_LENGTH = 23;
	private static final int MAX_TWEET_LENGTH = 140;
	private static final Logger LOG = LogManager.getLogger(TwitterExporter.class);
	
	@Override
	public void export(Post post, SocialConnection target) throws Exception {
		if(!SocialConnectionType.twitter.equals(target.getType())) {
			LOG.error("Cannot make a Twitter post to a Non-Twitter connection");
			return;
		}
		
		Connection<Twitter> twitter = connectionRepo.getConnection(target.getUserId(), Twitter.class);
		post.removeTag(target.getPumpTag());
		String message = postFromSocial(post);
		
		TweetData tweet = new TweetData(message.toString());
		
		if(StringUtils.isNotBlank(post.getImageUrl())) {
			try {
				Resource photoResource = new UrlResource(post.getImageUrl());
				tweet.withMedia(photoResource);
			}
			catch (IOException e) {
				LOG.error("Failed to get Image", e);
			}
		}
		
		twitter.getApi().timelineOperations().updateStatus(tweet);
	}

	private String postFromSocial(Post post) {
		String content = post.getContent();
		
		if(content == null && post.getLink() != null) {
			return post.getLink();
		}
		else if(content == null) {
			return "";
		}
		
		String link;
		
		if(CollectionUtils.isNotEmpty(post.getExternalLinks())) {
			// we'll post the first link at the end of the tweet
			ExternalLink externalLink = post.getExternalLinks().get(0);
			link = externalLink.getLink();
			
			// Remove external links from inside tweet
			for(ExternalLink curExtLink : post.getExternalLinks()) {
				String externalLinkStr = curExtLink.getLink();
				
				// remove the leading space if we can
				content = Utils.replace(content, " " + externalLinkStr, "");
				content = Utils.replace(content, externalLinkStr + " ", "");
				content = Utils.replace(content, externalLinkStr, "");
			}
		}
		else {
			link = post.getLink();
		}
		
		// need 23 characters space for link
		if(content.length() > (MAX_TWEET_LENGTH - LINK_LENGTH) && StringUtils.isNotBlank(post.getImageUrl())) {
			return content.substring(0, (MAX_TWEET_LENGTH - (LINK_LENGTH * 2) - 2)) + ".." + link;
		}
		else if(content.length() > MAX_TWEET_LENGTH) {
			return content.substring(0, (MAX_TWEET_LENGTH - LINK_LENGTH - 2)) + ".." + link;
		}
		else if(CollectionUtils.isNotEmpty(post.getExternalLinks()) && content.length() + LINK_LENGTH + 1 > MAX_TWEET_LENGTH) {
			return content.substring(0, (MAX_TWEET_LENGTH - LINK_LENGTH - 2)) + ".." + link;
		}
		else if(CollectionUtils.isNotEmpty(post.getExternalLinks())) {
			return content + " " + link;
		}
		
		return content;
	}
}