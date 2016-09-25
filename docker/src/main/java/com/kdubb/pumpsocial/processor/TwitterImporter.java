package com.kdubb.pumpsocial.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.twitter.api.MediaEntity;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.UrlEntity;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.ExternalLink.LinkType;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class TwitterImporter extends AbstractImporter {
	
	@Inject
	private OfflineConnectionRepository offlineRepo;
	
	private static final Logger LOG = LogManager.getLogger(TwitterImporter.class);
	
	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if(!SocialConnectionType.twitter.equals(source.getType())) {
			LOG.error("Cannot get Twitter feed for Non-Twitter connection");
			return null;
		}
		
		Collection<Post> result = new ArrayList<Post>();
		Twitter twitter = offlineRepo.getConnectionApi(source.getUserId(), Twitter.class);
		twitter.timelineOperations().getUserTimeline().stream()
			.filter(tweet -> {
				List<String> tags = Utils.findTags(tweet.getText());
				return tags.contains(source.getPumpTag());
			})
			.forEach(tweet -> {
				List<String> tags = Utils.findTags(tweet.getText());
				
				Post post = new Post();
				post.setSource(source);
				post.setUserId(source.getUserId());
				post.setContent(tweet.getText());
				post.setTags(tags);
				
				// If there's a media with a mediaUrl then attach a photo
				Optional<MediaEntity> optMedia = tweet.getEntities().getMedia().stream()
					.filter(x -> StringUtils.isNotBlank(x.getMediaUrl()))
					.findFirst();
				
				if(optMedia.isPresent()) {
					MediaEntity media = optMedia.get();
					post.setImageUrl(media.getMediaUrl());
					post.setLink(media.getExpandedUrl());
					
					if(StringUtils.isNotBlank(media.getUrl())) {
						String content = Utils.replace(post.getContent(), media.getUrl(), "");
						post.setContent(content.trim());
					}
				}
				else {
					post.setLink(getTweetUrl(tweet.getId(), tweet.getFromUser()));
				}
				
				// If there's links in the tweet, replace the shortened links and save 
				List<UrlEntity> urls = tweet.getEntities().getUrls();
				
				if(CollectionUtils.isNotEmpty(urls)) {
					List<ExternalLink> extLinks = new ArrayList<>();
					
					urls.stream().forEach(url -> {
						String shortened = url.getUrl();
						String realUrl = url.getExpandedUrl();
						
						String content = Utils.replace(post.getContent(), shortened, realUrl);
						post.setContent(content);

						ExternalLink link = new ExternalLink();
						link.setLink(realUrl);
						extLinks.add(link);
					});
					
					post.setExternalLinks(extLinks);
				}
				
				result.add(post);
			});
		
		return result;
	}
	
	private String getTweetUrl(long id, String fromUser) {
		return "http://twitter.com/" + fromUser + "/status/" + id + "/";
	}
}
