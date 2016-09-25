package com.kdubb.pumpsocial.service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.kdubb.pumpsocial.factory.AwsFactory;

@Service
public class EmailService {

	@Value("${ses.email.fromName}")
	private String fromName;

	@Value("${ses.email.fromAddress}")
	private String fromEmailAddress;
	
	@Inject
	private AwsFactory awsFactory;
	
	private Map<String, Template> templateMap = new HashMap<String, Template>();
	
	private final VelocityEngine engine = new VelocityEngine();
	private static final Logger LOG = LogManager.getLogger(EmailService.class);
	
	@PostConstruct
    public void init() {
        engine.setProperty("resource.loader", "class");
        engine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        engine.init();
    }
	
	private Template getTemplate(String templateName) {
        Template template = templateMap.get(templateName);

        if(template == null) {
            template = engine.getTemplate(templateName);
            // TODO bring this back when ready to cache
//    		templateMap.put(templateName, template);
        }

        return template;
    }
	
	public void sendWelcomeEmail(String emailAddress, String name) throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("userName", name);
		
		sendEmail(Arrays.asList(emailAddress), "Welcome to Pump Social", "email/welcome.vm", context);
	}
	
	public void sendAuthExceptionEmail(String emailAddress, String networkName, String connName) throws IOException {
		VelocityContext context = new VelocityContext();
		context.put("networkName", networkName);
		context.put("connName", connName);
		
		sendEmail(Arrays.asList(emailAddress), "Houston, there's a problem with your " + networkName, "email/authException.vm", context);
	}
	
	private Collection<String> sendEmail(Collection<String> bccAddresses, String subject, String template, VelocityContext context) throws IOException {
        String htmlBody = null;
        Template velocityTemplate = getTemplate(template);

        LocalDateTime curTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        context.put("constant-year", curTime.getYear());
        
        try(StringWriter writer = new StringWriter()) {
            velocityTemplate.merge(context, writer);
            htmlBody = writer.toString();
        } catch (IOException e){
            LOG.error("Error while render email body", e);
        }

        // This will be email sender as 'NAME <email@domain>'
        String source = fromName + " <" + fromEmailAddress + ">";

        final SendEmailRequest request = new SendEmailRequest();
        request.setSource(source);
		Destination destination = new Destination();

		if(CollectionUtils.isEmpty(bccAddresses))
			return null;
		
		String messageUuid = UUID.randomUUID().toString();
		
		// TODO use this in the email for a link
		// Extra unused uuid added for extra security, can switch this to another field for verification (ie. email subject)
//		String encodedUuid = EncryptionUtil.getBase64Encoded(messageUuid, Long.toString(campaign.getId()));
//		LOG.info("encodedUuid={" + encodedUuid + "}");
//
		Set<String> sentEmailMessageIds = new HashSet<>();

		for(String address : bccAddresses) {
			List<String> addresses = new ArrayList<String>();
			addresses.add(address);
			
			destination.setToAddresses(addresses);
			request.setDestination(destination);
			
	        Message message = new Message()
                    .withSubject(new Content().withData(subject))
                    .withBody(new Body().withHtml(new Content().withData(htmlBody)));

			request.setMessage(message);

			LOG.info("Going to send email to [" + address + "]");
			SendEmailResult result = awsFactory.getAmazonSimpleEmailServiceClient().sendEmail(request);
            LOG.info("MessageId=" + result.getMessageId());
            sentEmailMessageIds.add(result.getMessageId());
		}
		
        return sentEmailMessageIds;
    }
}