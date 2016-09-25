package com.kdubb.pumpsocial.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.social.NotAuthorizedException;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.mongodb.MongoConnectionService;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.plus.ActivitiesPage;
import org.springframework.social.google.api.plus.Activity;
import org.springframework.social.google.api.plus.Activity.Article;
import org.springframework.social.google.api.plus.Activity.Attachment;
import org.springframework.social.google.api.plus.Activity.Photo;
import org.springframework.social.google.api.plus.Activity.Video;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.ExternalLink.LinkType;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.enums.PostType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class GooglePlusImporter extends AbstractImporter {
	
	@Inject
	private OfflineConnectionRepository offlineRepo;
	
	@Inject
	private MongoConnectionService mongoConnectionService;
	
	private static final Logger LOG = LogManager.getLogger(GooglePlusImporter.class);

	public static enum ImageTypes {
		low_resolution, thumbnail, standard_resolution
	}
	
	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if(!SocialConnectionType.google.equals(source.getType())) {
			LOG.error("Cannot get Google feed for Non-Google connection");
			return null;
		}

		Collection<Post> result = new ArrayList<Post>();
		Connection<Google> googleConnection = offlineRepo.getConnection(source.getUserId(), Google.class);
		
		if(StringUtils.isNotBlank(source.getConnection().getRefreshToken()) && googleConnection.hasExpired()) {
			LOG.info("Refreshing Google Connection");
			googleConnection.refresh();
			mongoConnectionService.update(source.getUserId(), googleConnection);
		}
		
		Google google = googleConnection.getApi();
		
		try {
			String id = google.plusOperations().getGoogleProfile().getId();
			ActivitiesPage activityPage = google.plusOperations().getActivities(id);
			
			for(Activity activity : activityPage.getItems()) {
				String content = Utils.cleanPostText(activity.getContent());
				List<String> tags = Utils.findTags(content);
				
				if(!tags.contains(source.getPumpTag()))
					continue;
				
				Post post = new Post();
				post.setSource(source);
				post.setUserId(source.getUserId());
				post.setContent(content);
				post.setTags(tags);
				
				if(StringUtils.isNotBlank(activity.getUrl()))
					post.setLink(activity.getUrl());
				
				// search for external links in the text
				List<ExternalLink> links = new ArrayList<>();
				
				if(!CollectionUtils.isEmpty(activity.getAttachments())) {
					for(Attachment attachment : activity.getAttachments()) {
						if(StringUtils.isBlank(post.getImageUrl()) && attachment.getImage() != null) {
							post.setImageUrl(attachment.getImage().getUrl());
						}
						
						// This is all we need for photos, no links
						if(Photo.class.isAssignableFrom(attachment.getClass())) {
							continue;
						}
						
						ExternalLink extLink = new ExternalLink();
						extLink.setLink(attachment.getUrl());
						
						if(Article.class.isAssignableFrom(attachment.getClass())) {
							extLink.setType(LinkType.HTML);
						}
						else if(Video.class.isAssignableFrom(attachment.getClass())) {
							extLink.setType(LinkType.VIDEO);
							post.setType(PostType.VIDEO);
						}
						
						links.add(extLink);
					}
				}
				
				// If no links, try to get from content
				if(CollectionUtils.isEmpty(links)) {
					List<ExternalLink> contentLinks = getExternalLinksFromContent(activity.getContent());
					
					if(!CollectionUtils.isEmpty(contentLinks))
						links.addAll(contentLinks);
				}
				
				if(!CollectionUtils.isEmpty(links))
					post.setExternalLinks(links);
				
				result.add(post);
			}
			
			return result;
		}
		catch(HttpClientErrorException e) {
			if("401 Unauthorized".equals(e.getMessage())) {
				LOG.error("Google unauthorized", e);
				throw new NotAuthorizedException(source.getConnection().getProviderId(), "Google unauthorized");
			}
			
			throw e;
		}
	}
	
	private List<ExternalLink> getExternalLinksFromContent(String content) {
		List<ExternalLink> links = new ArrayList<>();
		Document doc = Jsoup.parse(content);
		Elements linkElements = doc.getElementsByTag("a");
		
		for(int i = 0; i < linkElements.size(); i++) {
			Element element = linkElements.get(i);
			
			String linkText = element.text();
			linkText = Utils.cleanTextOfNegativeBytes(linkText);
			
			// two different ascii hashtags
			if(!(linkText.startsWith("#") || linkText.startsWith("#")) && !element.hasClass("proflink")) {
				ExternalLink link = new ExternalLink();
				link.setLink(element.text());
				links.add(link);
			}
		}
		
		return links;
	}
}