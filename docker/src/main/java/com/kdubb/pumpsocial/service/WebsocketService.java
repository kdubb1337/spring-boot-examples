package com.kdubb.pumpsocial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.kdubb.pumpsocial.domain.response.AppState;
import com.kdubb.pumpsocial.domain.response.Notification;
import com.kdubb.pumpsocial.util.Utils;

@Service
public class WebsocketService {

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

	@Autowired
	private SocialConnectionService socialConnectionService;

	private static final Logger LOG = LoggerFactory.getLogger(WebsocketService.class);

	public void updateState(String userId) {

		AppState state = socialConnectionService.getAppState(userId);
		messagingTemplate.convertAndSendToUser(userId, "/queue/state", state);
	}

	public void notify(String userId, String message, Object body) {

		Notification notification = new Notification();
		notification.setTitle(message);
		notification.setBody(body);

		LOG.info("notify --> " + Utils.toPrettyJson(notification));
		messagingTemplate.convertAndSendToUser(userId, "/queue/notify", notification);
	}
}
