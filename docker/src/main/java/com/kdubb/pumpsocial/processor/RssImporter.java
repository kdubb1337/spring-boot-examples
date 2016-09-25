package com.kdubb.pumpsocial.processor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

@Service
public class RssImporter extends AbstractImporter {
	
	private static final Logger LOG = LogManager.getLogger(RssImporter.class);

	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if(!SocialConnectionType.rss.equals(source.getType())) {
			LOG.error("Cannot get RSS feed for Non-RSS connection");
			return null;
		}
		
		LOG.info("Checking [" + source.getTypeId() + "] for updates");
		URL url = new URL(source.getTypeId());
		HttpURLConnection httpcon = (HttpURLConnection)url.openConnection();
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(httpcon));
		
		@SuppressWarnings("unchecked")
		List<SyndEntry> entries = feed.getEntries();
		Date scrapeTime = Calendar.getInstance().getTime();
//		LOG.info("Entry:" + Utils.toPrettyJson(entry.getDescription()));
		List<Post> result = new ArrayList<Post>();
		
		for(SyndEntry entry : entries) {
			Post post = new Post();
			post.setSource(source);
//			post.setScrapeTime(scrapeTime);
			
//			LOG.info("Entry.description:" + Utils.toPrettyJson(entry.getDescription()));
//			
//			LOG.info("Entry.getUri:" + Utils.toPrettyJson(entry.getUri()));
//			LOG.info("Entry.getLink:" + Utils.toPrettyJson(entry.getLink()));
//			LOG.info("Entry.getLinks:" + Utils.toPrettyJson(entry.getLinks()));

			if(StringUtils.isNotBlank(entry.getLink()))
				post.setLink(entry.getLink());
			else if(StringUtils.isNotBlank(entry.getUri()))
				post.setLink(entry.getUri());
			
			if(!CollectionUtils.isEmpty(entry.getContents())) {
				SyndContent content = (SyndContent) entry.getContents().get(0);
				setCleanedContent(content, post);
			}
			else if(entry.getDescription() != null && StringUtils.isNotBlank(entry.getDescription().getValue())) {
				setCleanedContent(entry.getDescription(), post);
			}
			
			post.setTitle(entry.getTitle());
//			post.setPublishTime(entry.getPublishedDate());
			
			post = cleanPost(post);
			result.add(post);
		}
//		LOG.info("Posts:" + Utils.toPrettyJson(result));
		return result;
	}
	
	private void setCleanedContent(SyndContent content, Post post) {
		String contentValue = content.getValue();
		
		if("html".equals(content.getType()) || "text/html".equals(content.getType())) {
			Document doc = Jsoup.parse(contentValue);
			Elements images = doc.select("img");
			
			if(images.size() > 0 && !"1".equals(images.get(0).attr("width")) && !"1".equals(images.get(0).attr("height"))) {
				post.setImageUrl(images.get(0).attr("src"));
			}
			
			contentValue = doc.text();
			
			while(contentValue.contains("<iframe")) {
				doc = Jsoup.parse(contentValue);
				contentValue = doc.text();
			}
		}
		
		post.setContent(contentValue);
	}
}