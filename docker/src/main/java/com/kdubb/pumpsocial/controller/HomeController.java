package com.kdubb.pumpsocial.controller;

import java.security.Principal;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import com.kdubb.pumpsocial.domain.response.AppState;
import com.kdubb.pumpsocial.service.SocialConnectionService;
import com.kdubb.pumpsocial.service.WebsocketService;

@Controller
public class HomeController {

	@Inject
	private WebsocketService websocketService;

	@Inject
	private SocialConnectionService socialConnectionService;

	private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

	@RequestMapping("")
	public String home(Principal currentUser, Model model, HttpServletRequest request, HttpServletResponse response) {
		LOG.info("home request.getRequestedSessionId(): [{}]", request.getRequestedSessionId());
		LOG.info("currentUser.getName(): [{}]", currentUser.getName());
		return "index";
	}

	@RequestMapping("/privacy")
	public String privacy(Principal currentUser, Model model) {
		return "privacy";
	}

	@ResponseBody
	@RequestMapping("/redirect")
	public ModelAndView error(Principal currentUser, Model model, HttpServletRequest request) {
		LOG.error("Testing ERROR");
		request.getSession().invalidate();
		return new ModelAndView("redirect:/warning");
	}

	@ResponseBody
	@RequestMapping("/push/user")
	public String push(Principal currentUser, Model model, HttpServletRequest request) throws Exception {
		LOG.info("currentUser.getName(): [{}]", currentUser.getName());
		websocketService.updateState(currentUser.getName());
		return currentUser.getName();
	}

	// Initial subscription
	@SubscribeMapping("/initial")
	public AppState subscribeWebsockets(Principal currentUser) throws Exception {
		LOG.info("Subscibed for websockets for [{}]", currentUser.getName());
		return socialConnectionService.getAppState(currentUser.getName());
	}

	@ResponseBody
	@ExceptionHandler(HttpSessionRequiredException.class)
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "The session has expired")
	public String handleSessionExpired() {
		LOG.info("sessionExpired!!");
		return "sessionExpired";
	}
}