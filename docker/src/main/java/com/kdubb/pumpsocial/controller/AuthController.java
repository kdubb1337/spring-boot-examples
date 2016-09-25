package com.kdubb.pumpsocial.controller;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kdubb.pumpsocial.util.Utils;

@Controller
@RequestMapping("/signin")
public class AuthController {

	private static final Logger LOG = LogManager.getLogger(AuthController.class);

	@RequestMapping("")
	public String signin(HttpServletRequest request, HttpServletResponse response, Model model, Principal currentUser) {
		LOG.info("signin currentUser: [" + currentUser + "]");
		LOG.info("signin request.getRequestedSessionId(): [" + request.getRequestedSessionId() + "]");
		Map<String, Object> map = model.asMap();

		Enumeration<String> names = request.getSession().getAttributeNames();
		// LOG.info("model=" + Utils.toPrettyJson(response.getHeaderNames()));
		// LOG.info("X-Content-Type-Options=" + Utils.toPrettyJson(response.getHeaders("X-Content-Type-Options")));
		// LOG.info("X-XSS-Protection=" + Utils.toPrettyJson(response.getHeaders("X-XSS-Protection")));
		// LOG.info("Cache-Control=" + Utils.toPrettyJson(response.getHeaders("Cache-Control")));
		// LOG.info("Pragma=" + Utils.toPrettyJson(response.getHeaders("Pragma")));
		// LOG.info("Expires=" + Utils.toPrettyJson(response.getHeaders("Expires")));
		// LOG.info("X-Frame-Options=" + Utils.toPrettyJson(response.getHeaders("X-Frame-Options")));

		while (names.hasMoreElements())
			LOG.info("model2=" + Utils.toPrettyJson(names.nextElement()));

		for (String key : map.keySet())
			LOG.info("key=" + Utils.toPrettyJson(key));

		response.addHeader("page", "signin");
		return "signin2";
	}
}
