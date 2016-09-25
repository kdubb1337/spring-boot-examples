package com.kdubb.pumpsocial.processor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.domain.facebook.FacebookWall;
import com.kdubb.pumpsocial.domain.facebook.FacebookWallEntry;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;
import com.kdubb.pumpsocial.service.CrawledPageService;
import com.kdubb.pumpsocial.service.FacebookRssService;
import com.kdubb.pumpsocial.util.Utils;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.io.FeedException;

@Service
public class FacebookImporter extends AbstractImporter {

	@Inject
	private OfflineConnectionRepository offlineRepo;

	@Inject
	private FacebookRssService facebookRssService;

	@Inject
	private CrawledPageService crawledPageService;

	private static final Logger LOG = LogManager.getLogger(FacebookImporter.class);

	@Override
	public Collection<Post> process(SocialConnection source) throws Exception {
		if (!SocialConnectionType.facebook.equals(source.getType())) {
			LOG.error("Cannot get Facebook feed for Non-Facebook connection");
			return null;
		}

		Collection<Post> result = new ArrayList<Post>();
		// Facebook facebook = offlineRepo.getConnectionApi(source.getUserId(), Facebook.class);
		// // PagedList<org.springframework.social.facebook.api.Post> posts;
		// //
		// // // RSS only works on pages
		// // if(source.getParent() != null) {
		// // return processAll(source);
		// // }
		// //
		// // return null;
		//
		// // TODO requires read_stream ... facebook rejected us so far... fuckers
		//
		// PagedList<org.springframework.social.facebook.api.Post> posts = null;
		//
		// // // If main account
		// if (source.getParent() == null) {
		// posts = facebook.feedOperations().getFeed();
		// }
		// // If page
		// else {
		// posts = facebook.feedOperations().getFeed(source.getTypeId());
		// }
		//
		// LOG.info("posts --> " + Utils.toPrettyJson(posts));
		//
		// posts.stream().filter(fbPost -> {
		// List<String> tags = Utils.findTags(fbPost.getMessage());
		// return tags.contains(source.getPumpTag());
		// }).forEach(fbPost -> {
		// List<String> tags = Utils.findTags(fbPost.getMessage());
		//
		// Post post = new Post();
		// post.setSource(source);
		// post.setUserId(source.getUserId());
		// post.setContent(fbPost.getMessage());
		// post.setTags(tags);
		//
		// // Ensure we have a link to avoid duplicates no matter what
		// if (StringUtils.isNotBlank(fbPost.getLink()))
		// post.setLink(fbPost.getLink());
		// else if (!CollectionUtils.isEmpty(fbPost.getActions())) {
		// post.setLink(fbPost.getActions().get(0).getLink());
		// } else {
		// post.setLink("FacebookID:" + fbPost.getId());
		// }
		//
		// if (PostType.PHOTO.equals(fbPost.getType())) {
		// // Find the largest image (since FB creates multiple sizes) [Note: might fail if they don't have
		// // user_photo privilage]
		// Optional<Image> image =
		// facebook.mediaOperations().getPhoto(fbPost.getObjectId()).getImages().stream().max((a, b) -> {
		// return a.getWidth() - b.getWidth();
		// });
		//
		// if (image.isPresent())
		// post.setImageUrl(image.get().getSource());
		// }
		//
		// // Remove the triggering tag
		// post.removeTag(source.getPumpTag());
		// result.add(post);
		// });

		return result;
	}

	public Collection<Post> processAll(SocialConnection source) throws Exception {
		// RSS only works on pages
		if (source.getParent() != null) {
			try {
				return crawlRssFeed(source, source.getTypeId(), false);
			}
			catch (Exception e) {
				try {
					return crawlJsonFeed(source, source.getTypeId(), false);
				}
				catch (Exception e1) {
					LOG.warn("Failed to getWall for FB id=[" + source.getTypeId() + "]");
				}
			}
		}

		LOG.warn("Facebook pages aren't being read!!");
		return new ArrayList<>();
	}

	public static void main(String[] args) throws IllegalArgumentException, IOException, FeedException {
		List<SyndEntry> entries = Utils.getRssEntries("https://kat.cr/tv/?rss=1");

		for (SyndEntry entry : entries) {
			LOG.info("RSS crawl: [" + entry.getLink() + "]");
		}
	}

	private Collection<Post> crawlRssFeed(SocialConnection source, String typeId, boolean requireTag)
			throws IOException, IllegalArgumentException, FeedException {
		List<Post> result = new ArrayList<Post>();

		List<SyndEntry> entries = Utils.getRssEntries("https://www.facebook.com/feeds/page.php?format=rss20&id=" + typeId);
		// LOG.info("https://www.facebook.com/feeds/page.php?format=rss20&id=" + typeId);

		for (SyndEntry entry : entries) {
			if (crawledPageService.isPageCrawled(entry.getLink())) {
				continue;
			}

			LOG.info("FB RSS crawl: [" + entry.getLink() + "]");
			Post post = getPostFromUrl(entry.getLink(), source, requireTag);

			if (post != null)
				result.add(post);
		}

		return result;
	}

	private Collection<Post> crawlJsonFeed(SocialConnection source, String typeId, boolean requireTag) throws IOException {
		List<Post> result = new ArrayList<Post>();
		FacebookWall wall = facebookRssService.getWall("json", typeId);

		if (wall == null)
			return result;

		for (FacebookWallEntry entry : wall.getEntries()) {
			if (crawledPageService.isPageCrawled(entry.getAlternate())) {
				continue;
			}

			LOG.info("FB JSON crawl: [" + entry.getAlternate() + "]");
			Post post = getPostFromUrl(entry.getAlternate(), source, requireTag);

			if (post != null)
				result.add(post);
		}

		return result;
	}

	private Post getPostFromUrl(String url, SocialConnection source, boolean requireTag) throws IOException {
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		Element photo = doc.getElementById("fbPhotoImage");
		Element text = null;
		Post post = new Post();

		if (url.contains("video.php")) {
			String videoUrl = getVideoLink(doc.html());

			if (StringUtils.isNotBlank(videoUrl)) {
				post.setVideoUrl(videoUrl);
			}

			text = doc.getElementById("fbPhotoPageCaption");
		} else if (photo != null) {
			post.setImageUrl(photo.attr("src"));
			text = doc.getElementById("fbPhotoPageCaption");
		} else {
			Element contentArea = doc.getElementById("contentArea");

			if (contentArea != null)
				text = contentArea.getElementsByClass("userContent").first();

			// TODO hacky
			if (text == null) {
				// Sometimes they throw a few in there to mess us up, find the real one
				Elements elements = doc.getElementsByClass("hidden_elem");

				for (int i = 0; i < elements.size(); i++) {
					Element element = elements.get(i);

					if (element.html() == null) {
						continue;
					}

					String html = element.html().trim();

					if (html.length() < 7) {
						continue;
					}

					html = html.substring(4, html.length() - 3);

					Document hiddenDoc = Jsoup.parse(html);
					text = hiddenDoc.getElementsByClass("userContent").first();

					if (text != null) {
						break;
					}
				}
			}
		}

		if (text == null) {
			failedToParse(url, doc.body().html());
			return null;
		}

		// if there are links in the text
		Elements linkElements = text.getElementsByTag("a");
		Map<String, String> linkMap = new HashMap<>();

		for (int i = 0; i < linkElements.size(); i++) {
			Element element = linkElements.get(i);

			String linkText = element.text();
			linkText = Utils.cleanTextOfNegativeBytes(linkText);

			// two different ascii hashtags
			if (!(linkText.startsWith("#") || linkText.startsWith("#"))) {
				String href = element.attr("href");
				href = Utils.getParamenterFromUrl(href, "u"); // href is a facebook redirect to the real link
				linkMap.put(element.text(), href);
			}
		}

		String content = Utils.cleanPostText(text.text());

		if (linkMap.size() > 0) {
			List<String> links = new ArrayList<>();
			String[] resultContent = new String[] { content };

			linkMap.entrySet().stream().forEach(linkEntry -> {
				links.add(linkEntry.getValue());
				resultContent[0] = Utils.replace(resultContent[0], linkEntry.getKey(), linkEntry.getValue());
			});

			content = resultContent[0];
		}

		// // Clean up negative bytes
		// for(Entry<String, String> hash : hashMap.entrySet()) {
		// content = Utils.replace(content, hash.getKey(), hash.getValue());
		// }

		List<String> tags = Utils.findTags(content);

		if (requireTag && !tags.contains(source.getPumpTag()))
			return null;

		post.setLink(url);
		post.setSource(source);
		post.setUserId(source.getUserId());
		post.setContent(content);
		post.setTags(tags);

		return post;
	}

	private String getVideoLink(String html) throws UnsupportedEncodingException {
		int index = html.indexOf("[[\"params");
		html = html.substring(index);
		int index2 = html.indexOf("]]");
		String toDecode = html.substring(0, index2 + 2);

		Collection<?> toDecodeColl = Utils.fromJson(toDecode, Collection.class);

		for (Object innerObj : toDecodeColl) {
			if (List.class.isAssignableFrom(innerObj.getClass())) {
				List<String> list = (List<String>) innerObj;
				Map<String, String> horribleMap = new HashMap<>();

				for (int i = 0; i < list.size() - 1; i += 2) {
					horribleMap.put(list.get(i), list.get(i + 1));
				}

				if (horribleMap.containsKey("params")) {
					String params = URLDecoder.decode(horribleMap.get("params"), "UTF-8");
					Map<String, Object> realMap = (Map<String, Object>) Utils.fromJson(params, Map.class);

					List<Map<String, String>> videoDataList = (List<Map<String, String>>) realMap.get("video_data");
					Map<String, String> videoDataMap = videoDataList.get(0);

					String hd = videoDataMap.get("hd_src");
					return StringUtils.isBlank(hd) ? videoDataMap.get("sd_src") : hd;
				}
			}
		}

		return null;
	}

	private void failedToParse(String link, String html) {
		LOG.warn("Failed to parse FB post " + link + "\n\n" + html);
		crawledPageService.setPageCrawled(link, true);
	}
}
