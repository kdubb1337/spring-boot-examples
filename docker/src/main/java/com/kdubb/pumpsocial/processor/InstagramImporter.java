package com.kdubb.pumpsocial.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.social.instagram.api.Image;
import org.springframework.social.instagram.api.Instagram;
import org.springframework.social.instagram.api.Media;
import org.springframework.social.instagram.api.PagedMediaList;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class InstagramImporter extends AbstractImporter {
	
	@Inject
	private OfflineConnectionRepository offlineRepo;
	
	private static final Logger LOG = LogManager.getLogger(InstagramImporter.class);

	public static enum ImageTypes {
		low_resolution, thumbnail, standard_resolution
	}
	
	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if(!SocialConnectionType.instagram.equals(source.getType())) {
			LOG.error("Cannot get Instagram feed for Non-Instagram connection");
			return null;
		}
		
		Instagram instagram = offlineRepo.getConnectionApi(source.getUserId(), Instagram.class);
		PagedMediaList list = instagram.userOperations().getRecentMedia(instagram.userOperations().getUser().getId());
		
		Collection<Post> result = new ArrayList<Post>();
		
		for(Media media : list.getList()) {
//			LOG.info("media --> " + Utils.toPrettyJson(media));
			
			for(String tag : media.getTags()) {
				if(!source.getPumpTag().equalsIgnoreCase(tag)) {
					continue;
				}
				
				Post post = new Post();
				post.setSource(source);
				post.setUserId(source.getUserId());
				
				Image image = media.getImages().get(ImageTypes.standard_resolution.toString());
				
				if(image != null)
					post.setImageUrl(image.getUrl());
				
				post.setLink(media.getLink());
				post.setContent(media.getCaption().getText());
				post.setTags(media.getTags());
				
				String videoUrl = getVideoUrl(media.getLink());
				
				if(StringUtils.isNotBlank(videoUrl))
					post.setVideoUrl(videoUrl);
				
				List<String> linksInContent = Utils.findLinks(post.getContent());
				
				if(CollectionUtils.isNotEmpty(linksInContent)) {
					List<ExternalLink> extLinks = new ArrayList<>();
					
					for(String link : linksInContent) {
						ExternalLink externalLink = new ExternalLink();
						externalLink.setLink(link);
						extLinks.add(externalLink);
					}
					
					post.setExternalLinks(extLinks);
				}
				
				result.add(post);
				
				break;
			}
		}
		
		return result;
	}

	private String getVideoUrl(String link) {
		if(StringUtils.isBlank(link))
			return null;
		
		try {
			Document doc = Jsoup.connect(link).get();
			Element videoElement = doc.select("meta[property$=og:video]").first();
			
			if(videoElement == null)
				return null;
			
			return videoElement.attr("content");
		} catch(IOException e) {
			LOG.error("Failed to getVideoUrl for [" + link + "]", e);
			return null;
		}
	}
}