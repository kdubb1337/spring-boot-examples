package com.kdubb.pumpsocial.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kdubb.pumpsocial.util.url.URL;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class Utils {
	private static final Logger LOG = LogManager.getLogger(Utils.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static {
		OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static <T> ResponseEntity<T> unsupportedMedia(T t) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "application/json");
		return new ResponseEntity<T>(t, responseHeaders, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	public static <T> ResponseEntity<T> httpOK(T t) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "application/json");
		return new ResponseEntity<T>(t, responseHeaders, HttpStatus.OK);
	}
	
	public static <T> ResponseEntity<T> httpError(T t) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "application/json");
		return new ResponseEntity<T>(t, responseHeaders, HttpStatus.NOT_ACCEPTABLE);
	}

	public static <T> T convertValue(Object obj, TypeReference<?> typeReference) {
		try {
			return OBJECT_MAPPER.convertValue(obj, typeReference);
		}
		catch (Exception e) {
			LOG.error("OBJECT_MAPPER convertValue error", e);
			return null;
		}
	}

	public static <T> T convertValue(Object obj, Class<T> classObj) {
		try {
			return OBJECT_MAPPER.convertValue(obj, classObj);
		}
		catch (Exception e) {
			LOG.error("OBJECT_MAPPER convertValue error", e);
			return null;
		}
	}

	public static <T> T convertValue(String str, Class<T> classObj) {
		try {
			return OBJECT_MAPPER.readValue(str, classObj);
		}
		catch (Exception e) {
			LOG.error("OBJECT_MAPPER convertValue error", e);
			return null;
		}
	}

	public static String toPrettyJson(Object obj) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			LOG.error("Failed to convert object toPrettyJson", e);
			return null;
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		
		try {
			return mapper.readValue(json, clazz);
		}
		catch (IOException e) {
			LOG.error("Failed to convert object toPrettyJson", e);
			return null;
		}
	}
	
	public static String getFileExtension(String filename) {
		if (StringUtils.isBlank(filename))
			return null;

		return FilenameUtils.getExtension(filename.toLowerCase());
	}

	public static boolean isValid(String... strings) {
		if (ArrayUtils.isEmpty(strings))
			return false;

		for (String str : strings)
			if (StringUtils.isEmpty(str))
				return false;

		return true;
	}

	public static String encodeUrl(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		}
		catch (Exception e) {
			LOG.error("Failed to encodeUrl [" + str + "]", e);
			return "";
		}
	}

	public static String encodeUrlPlus(String str) {
		return encodeUrl(str).replace("+", "%20");
	}
	
	public static String maxLength(String str, int maxLength) {
		if(StringUtils.isBlank(str) || maxLength < 3)
			return str;
		
		if(str.length() > maxLength)
			return str.substring(0, maxLength - 3) + "...";
			
		return str;
	}
	
	public static String removeTag(String str, String tag) {
		if(StringUtils.isBlank(str) || StringUtils.isBlank(tag)) {
			return str;
		}
		
		String fulltag = "#" + tag;
		
		if(str.equals(fulltag))
			return "";
 		
		String result = str;
 		
		Pattern pattern = Pattern.compile("(\\s|\\A|^)#(" + Pattern.quote(tag) + ")(\\s|\\Z|^)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(result);

		while(matcher.find()) {
			result = matcher.replaceFirst(matcher.group(3));
		}
 		
		return result;
	}
	
	public static List<String> findTags(String str) {
		return findTagsOriginal(str).stream()
				.map(x -> x.toLowerCase())
				.collect(Collectors.toList());
	}
	
	public static List<String> findTagsOriginal(String str) {
		List<String> result = new ArrayList<String>();
		
		if(StringUtils.isBlank(str))
			return result;

		String curString = cleanTextOfNegativeBytes(str);
		Pattern pattern = Pattern.compile("(\\s|\\A|^)[#|‪#](\\w+)(\\s|\\Z|^)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(curString);
		
		while(matcher.find()) {
			result.add(matcher.group(2));
			
			// TODO this should be unnecessary with a better knowledge of regex
			String remaining = curString.substring(matcher.end());
			
			if(StringUtils.isNotBlank(remaining)); {
				curString = remaining;
				matcher = pattern.matcher(remaining);
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static List<SyndEntry> getRssEntries(String url) throws IOException, IllegalArgumentException, FeedException {
		java.net.URL javaUrl = new java.net.URL(url);
		HttpURLConnection httpcon = (HttpURLConnection)javaUrl.openConnection();
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(httpcon));
		return feed.getEntries();
	}
	
	public static boolean isVideoLink(String link) {
		if(StringUtils.isBlank(link)) {
			return false;
		}

        Pattern pattern = Pattern.compile("(http(s)?://)?(www\\.)?([^/]*)");
        Matcher matcher = pattern.matcher(link);
        
        if(!matcher.find())
        	return false;
        
        String[] videoSites = new String[]{
        	"youtube.com",
        	"youtu.be",
        	"vimeo.com"
        };
        
        for(String site : videoSites) {
        	if(site.equals(matcher.group(4)))
        		return true;
        }
        
        return false;
	}
	
	public static String getEmbed(String link) {
		if(!isVideoLink(link))
			return null;
		
		Pattern pattern = Pattern.compile("(http(s)?://)?(www\\.)?([^/]*)");
        Matcher matcher = pattern.matcher(link);
        
        if(!matcher.find())
        	return null;
        
        String domain = matcher.group(4);
        
        switch(domain) {
        case "youtube.com":
        	Pattern pattern2 = Pattern.compile("(http(s)?://)?(www\\.)?([^/]*)/watch\\?v=([^&]*)");
            Matcher matcher2 = pattern2.matcher(link);
            
            if(!matcher2.find())
            	return null;
            
            return getYoutubeEmbed(matcher2.group(5));
        case "youtu.be":
        	Pattern pattern3 = Pattern.compile("(http(s)?://)?(www\\.)?([^/]*)/([^/]*)");
            Matcher matcher3 = pattern3.matcher(link);
            
            if(!matcher3.find())
            	return null;
            
            return getYoutubeEmbed(matcher3.group(5));
        case "vimeo.com":
        	Pattern pattern4 = Pattern.compile("(http(s)?://)?(www\\.)?([^/]+)(/.+)?/([^/]+)");
            Matcher matcher4 = pattern4.matcher(link);

            if(!matcher4.find())
            	return null;
            
            return getVimeoEmbed(matcher4.group(6));
        default:
        	return null;
        }
	}
	
	public static String getYoutubeEmbed(String youtubeId) {
		if(StringUtils.isBlank(youtubeId))
			return null;
		
		return "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/" + youtubeId + "\" frameborder=\"0\" allowfullscreen></iframe>";
	}
	
	public static String getVimeoEmbed(String vimeoId) {
		if(StringUtils.isBlank(vimeoId))
			return null;
		
		return "<iframe src=\"//player.vimeo.com/video/" + vimeoId + "?color=ff9933&badge=0\" width=\"500\" height=\"281\" frameborder=\"0\" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>";
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, IllegalArgumentException, FeedException {
		
		
		String url = "https://redirector.googlevideo.com/videoplayback?requiressl\u003dyes\u0026shardbypass\u003dyes\u0026cmbypass\u003dyes\u0026id\u003d57ff0fe7c349bf2f\u0026itag\u003d36\u0026source\u003dpicasa\u0026cmo\u003dsecure_transport%3Dyes\u0026ip\u003d0.0.0.0\u0026ipbits\u003d0\u0026expire\u003d1423625538\u0026sparams\u003drequiressl";
		LOG.info("url =" + url);
		
//		LOG.info("cleanPostText =" + cleanPostText("I used to think beavers were harmless. Must see <a rel=\"nofollow\" class=\"ot-hashtag\" href=\"https://plus.google.com/s/%23Pump\">#Pump</a><br   /><br /><a href=\"https://www.youtube.com/watch?v=y4t0BKlICsk\">https://www.youtube.com/watch?v=y4t0BKlICsk</a>﻿"));
		
//		String url = "https://www.facebook.com/video.php?v=320372044828844";
////		Document doc = Jsoup.connect(url).get();
////		doc.get
//		
//		String html = FileUtils.readFileToString(new File("D:\\Tmp\\test\\httpswwwfacebookcomvideophpv320372044828844.html"));
//		
////		LOG.info("readFile --> " + html);
//		
//		int index = html.indexOf("[[\"params");
//		html = html.substring(index);
//		int index2 = html.indexOf("]]");
//		String toDecode = html.substring(0, index2 + 2);
//		LOG.info("readFile --> " + toDecode);
//		
////		String toDecode = "[[\"params\",\"\u00257B\u002522auto_hd\u002522\u00253Afalse\u00252C\u002522autoplay_reason\u002522\u00253A\u002522unknown\u002522\u00252C\u002522default_hd\u002522\u00253Atrue\u00252C\u002522disable_native_controls\u002522\u00253Afalse\u00252C\u002522inline_player\u002522\u00253Afalse\u00252C\u002522pixel_ratio\u002522\u00253A1\u00252C\u002522preload\u002522\u00253Atrue\u00252C\u002522start_muted\u002522\u00253Afalse\u00252C\u002522video_data\u002522\u00253A\u00255B\u00257B\u002522hd_src\u002522\u00253A\u002522https\u00253A\u00255C\u00252F\u00255C\u00252Fscontent-b.xx.fbcdn.net\u00255C\u00252Fhvideo-xpf1\u00255C\u00252Fv\u00255C\u00252Ft43.1792-2\u00255C\u00252F10565381_10152840823292758_299650948_n.mp4\u00253Foh\u00253Dbaed5f1b0dbe0708ad2a1f97f6496209\u002526oe\u00253D54CEC8C1\u002522\u00252C\u002522is_hds\u002522\u00253Afalse\u00252C\u002522is_hls\u002522\u00253Afalse\u00252C\u002522rotation\u002522\u00253A0\u00252C\u002522sd_src\u002522\u00253A\u002522https\u00253A\u00255C\u00252F\u00255C\u00252Ffbcdn-video-e-a.akamaihd.net\u00255C\u00252Fhvideo-ak-xpa1\u00255C\u00252Fv\u00255C\u00252Ft42.1790-2\u00255C\u00252F10941100_320372178162164_463560033_n.mp4\u00253Foh\u00253D7c75acc978f0db0fe0ba8f92d853e375\u002526oe\u00253D54CEC8AA\u002526__gda__\u00253D1422839445_349564ee53a7416ffbc1042d7308bbb1\u002522\u00252C\u002522video_id\u002522\u00253A\u002522320372044828844\u002522\u00252C\u002522codec\u002522\u00253A\u002522sd\u00253A+h264\u00252C+hd\u00253A+h264\u002522\u00252C\u002522subtitles_src\u002522\u00253Anull\u00257D\u00255D\u00252C\u002522show_captions_default\u002522\u00253Afalse\u00252C\u002522persistent_volume\u002522\u00253Atrue\u00252C\u002522buffer_length\u002522\u00253A0.1\u00257D\"],[\"width\",\"720\"],[\"height\",\"720\"],[\"user\",\"0\"],[\"log\",\"no\"],[\"div_id\",\"id_54ceaa591961c9e97130133\"],[\"swf_id\",\"swf_id_54ceaa591961c9e97130133\"],[\"browser\",\"Unknown+0\"],[\"tracking_domain\",\"https\u00253A\u00252F\u00252Fpixel.facebook.com\"],[\"post_form_id\",\"\"],[\"string_table\",\"https\u00253A\u00252F\u00252Fs-static.ak.facebook.com\u00252Fflash_strings.php\u00252Ft98807\u00252Fen_US\"]]";
////		toDecode = URLDecoder.decode(toDecode, "UTF-8");
////		LOG.info("decodeTest=<<" + Utils.toPrettyJson(obj) + ">>");
////		toDecode = Utils.replace(toDecode, "\\/", "");
//		
//		Collection<?> toDecodeColl = fromJson(toDecode, Collection.class);
//		LOG.info("decodeTest=<<" + Utils.toPrettyJson(toDecodeColl) + ">>");
//		
//		for(Object innerObj : toDecodeColl) {
//			if(List.class.isAssignableFrom(innerObj.getClass())) {
//				List<String> list = (List<String>)innerObj;
//				Map<String, String> horribleMap = new HashMap<>();
//				
//				for(int i = 0; i < list.size() - 1; i += 2) {
//					horribleMap.put(list.get(i), list.get(i + 1));
//				}
//				
//				if(horribleMap.containsKey("params")) {
//					String params = URLDecoder.decode(horribleMap.get("params"), "UTF-8");
//					Map<String, Object> realMap = (Map<String, Object>) Utils.fromJson(params, Map.class);
//					LOG.info("params = "+ Utils.toPrettyJson(realMap));
//					List<Map<String, String>> videoDataList = (List<Map<String, String>>) realMap.get("video_data");
//					Map<String, String> videoDataMap = videoDataList.get(0);
//					LOG.info("hd=" + videoDataMap.get("hd_src"));
//					LOG.info("sd=" + videoDataMap.get("sd_src"));
//				}
//			}
//		}
	}

	public static List<String> findLinks(String str) {
		List<String> result = new ArrayList<String>();
		
		if(StringUtils.isBlank(str))
			return result;
		
		Pattern pattern = Pattern.compile("(\\s|\\A|^)((http(s)?://)|(www\\.))([^\\s]+)(\\s|\\Z|^)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);
        
        while(matcher.find()) {
        	result.add(matcher.group(2) + matcher.group(6));
        }
        
		return result;
	}
	
	public static String cleanPostText(String text) {
		if(StringUtils.isBlank(text))
			return null;
		
		String result = Jsoup.clean(text, Whitelist.none().addTags("br"));
		result = cleanTextOfNegativeBytes(result);
		result = result.replaceAll("&nbsp;", " ");
		result = result.replaceAll("<br />", "\n");
		
		// Remove odd character from G+
		result = result.replaceAll(" ﻿", "");
		result = result.replaceAll("\\s+", " ");
		return result.trim();
	}
	
	public static String getParamenterFromUrl(String url, String parameterName) {
		URL parsedUrl = new URL(url);
		return parsedUrl.getParameter(parameterName, null);
	}
	
	public static String cleanTextOfNegativeBytes(String text) {
		String result = "";
		
		for(Byte b : text.getBytes()) {
			if(b.intValue() >= 0)
				try {
					result += new String(new byte[]{b.byteValue()}, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOG.error("Shouldn't get here...", e);
				}
		}
		
		return result;
	}

	public static String replace(String text, String searchStr, String replacementStr) {
		return StringUtils.replaceEach(text, new String[]{searchStr}, new String[]{replacementStr});
	}

	public static String removeProtocol(String link) {
		return link.replaceAll("http(s)?://", "");
	}
}
