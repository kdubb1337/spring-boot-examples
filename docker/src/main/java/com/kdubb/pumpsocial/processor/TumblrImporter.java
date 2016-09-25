package com.kdubb.pumpsocial.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.social.tumblr.api.Photo;
import org.springframework.social.tumblr.api.PhotoSize;
import org.springframework.social.tumblr.api.PostType;
import org.springframework.social.tumblr.api.Tumblr;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.ExternalLink;
import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.ExternalLink.LinkType;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.util.Utils;
import com.kdubb.pumpsocial.util.url.URL;

@Service
public class TumblrImporter extends AbstractImporter {
	
	@Inject
	private OfflineConnectionRepository offlineRepo;
	
	private static final Logger LOG = LogManager.getLogger(TumblrImporter.class);
	
	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if(!SocialConnectionType.tumblr.equals(source.getType())) {
			LOG.error("Cannot get Tumblr feed for Non-Tumblr connection");
			return null;
		}
		
		Collection<Post> result = new ArrayList<Post>();
		Tumblr tumblr = offlineRepo.getConnectionApi(source.getUserId(), Tumblr.class);
		
		List<org.springframework.social.tumblr.api.Post> tumbles = tumblr.blogOperations(source.getTypeId()).blogPostsOperations().getPosts();
		
		tumbles.stream()
			.filter(tumble -> {
				List<String> tags = tumble.getTags();
				return tags.contains(source.getPumpTag());
			})
			.forEach(tumble -> {
//				LOG.info("tumble -> " + Utils.toPrettyJson(tumble));
				Post post = new Post();
				post.setSource(source);
				post.setUserId(source.getUserId());
				post.setTags(tumble.getTags());
				post.setLink(tumble.getPostUrl());
				
				try {
					if(tumble.getType() != null) {
						com.kdubb.pumpsocial.enums.PostType type = com.kdubb.pumpsocial.enums.PostType.valueOf(tumble.getType().toString());
						post.setType(type);
					}
					else {
						LOG.error("PostType is null for tumble " + tumble.getPostUrl());
					}
				}
				catch(Exception e) {
					LOG.error("Failed to parse Tumblr post type of [" + tumble.getType() + "]");
				}
				
				if(StringUtils.isEmpty(post.getContent())) {
					post.setContent(Utils.cleanPostText(tumble.getBody()));
				}
				
				List<Photo> photos = tumble.getPhotos();
				
				if(photos != null) {
					tumble.getPhotos().stream()
						.limit(1)
						.forEach(photo -> {
							Optional<PhotoSize> photoSize = photo.getSizes().stream()
								.max((a, b) -> a.getWidth() - b.getWidth());
							
							if(photoSize.isPresent()) {
								post.setImageUrl(photoSize.get().getUrl());
							}
						});
				}
				
				if(PostType.LINK.equals(tumble.getType())) {
					ExternalLink link = new ExternalLink();
					link.setLink(tumble.getUrl());
					link.setType(LinkType.HTML);
					
					post.setExternalLinks(Arrays.asList(link));
					
					URL linkUrl = new URL(tumble.getUrl());
					List<String> links = new ArrayList<>();
					links.add(linkUrl.toString());
					
					String content = tumble.getDescription();
					
					if(StringUtils.isNotBlank(content)) {
						Document doc = Jsoup.parse(content);
						doc.getElementsByClass("link_og_blockquote").remove();
						content = doc.text();
					}
					
					if(StringUtils.isBlank(content)) {
						content = tumble.getTitle();
					}
					
					content = content.trim();
					post.setContent(content);
				}
				else if(PostType.VIDEO.equals(tumble.getType())) {
					post.setContent(Utils.cleanPostText(tumble.getCaption()));
					
					String embedCode = tumble.getVideoPlayers().get(0).getEmbedCode();
					Document doc = Jsoup.parse(embedCode);
					
					Element iframe = doc.getElementsByTag("iframe").first();
					
					// If embedded
					if(iframe != null) {
						String src = iframe.attr("src");
						ExternalLink link = new ExternalLink();
						link.setLink(src);
						link.setType(LinkType.VIDEO);
						post.setExternalLinks(Arrays.asList(link));
					}
					// If uploaded
					else {
						Element videoElement = doc.getElementsByTag("video").first();
						Element sourceElement = doc.getElementsByTag("source").first();
						
						if(sourceElement != null) {
							String src = sourceElement.attr("src");
							
							if(src != null && !src.endsWith(".mp4")) {
								src += ".mp4";
							}
							
							post.setVideoUrl(src);
						}
						else {
							LOG.error("Failed to parse Tumblr embedCode=[" + embedCode + "]");
						}
						
						if(videoElement != null) {
							String imageUrl = videoElement.attr("poster");
							post.setImageUrl(imageUrl);
						}
					}
				}
				else {
					post.setExternalLinks(getExternalLinks(tumble.getCaption()));
					post.setContent(Utils.cleanPostText(tumble.getCaption()));
				}
				
				result.add(post);
			});
		
		return result;
	}
	
	private List<ExternalLink> getExternalLinks(String caption) {
		if(StringUtils.isBlank(caption))
			return null;
		
		Document doc = Jsoup.parse(caption);
		
		if(doc == null)
			return null;
		
		Elements linkElements = doc.getElementsByTag("a");
		
		if(linkElements == null || linkElements.size() < 1)
			return null;
		
		List<ExternalLink> links = new ArrayList<>();
		
		for(int i = 0; i < linkElements.size(); i++) {
			Element element = linkElements.get(i);
			
			String linkText = element.text();
			linkText = Utils.cleanTextOfNegativeBytes(linkText);
			
			// two different ascii hashtags
			if(!(linkText.startsWith("#") || linkText.startsWith("#"))) {
				ExternalLink link = new ExternalLink();
				link.setLink(element.text());
				links.add(link);
			}
		}
		
		if(CollectionUtils.isEmpty(links))
			return null;
		
		return links;
	}
}
