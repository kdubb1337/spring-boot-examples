package com.kdubb.pumpsocial.exporter;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.social.google.api.Google;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.Post;
import com.kdubb.pumpsocial.domain.SocialConnection;
import com.kdubb.pumpsocial.domain.enums.SocialConnectionType;
import com.kdubb.pumpsocial.repository.OfflineConnectionRepository;

@Service
public class GoogleExporter extends Exporter {
	
	@Inject
	private OfflineConnectionRepository connectionRepo;
	
	private static final Logger LOG = LogManager.getLogger(FacebookExporter.class);
	
	@Override
	public void export(Post post, SocialConnection target) throws Exception {
		if(!SocialConnectionType.google.equals(target.getType())) {
			LOG.error("Cannot make a Google post to a Non-Google connection");
			return;
		}
		
		@SuppressWarnings("unused")
		Google google = connectionRepo.getConnectionApi(target.getUserId(), Google.class);
		post.removeTag(target.getPumpTag());
		
		if(StringUtils.isNotBlank(target.getTypeId())) {
			if(StringUtils.isNotBlank(post.getLink())) {
				StringBuilder message = new StringBuilder();
				SocialConnection source = post.getSource();
				
				if(StringUtils.isNotBlank(source.getName()))
					message.append(source.getName());
				
				if(StringUtils.isNotBlank(post.getTitle())) {
					if(message.length() > 0)
						message.append(" - ");
					
					message.append(post.getTitle());
				}
				
				// TODO will need to be granted access to the pages API to manage social on G+
				
//				MomentTarget momentTarget = new MomentTarget("https://developers.google.com/+/web/snippet/examples/photo");
//				Moment moment = new CreateActivity("https://developers.google.com/+/web/snippet/examples/photo");
//				google.plusOperations().insertMoment(moment);
				
				
//				FacebookLink link = new FacebookLink(post.getLink(), null, null, null);
//				facebook.pageOperations().post(target.getTypeId(), message.toString(), link);
			}
		}
	}
}