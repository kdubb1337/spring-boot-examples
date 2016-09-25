package com.kdubb.pumpsocial.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class ErrorController implements org.springframework.boot.autoconfigure.web.ErrorController {
	
	private static final String PATH = "/error";
	private static final Logger LOG = LogManager.getLogger(ErrorController.class);

	@Override
	public String getErrorPath() {
		return PATH;
	}
	
	@RequestMapping(PATH)
	public ModelAndView error(@RequestParam(required = false) String error, Model model, HttpServletRequest request) {
		LOG.error("Catching error and redirecting [" + error + "]");
//		request.getSession().invalidate();
		return new ModelAndView("redirect:/");
	}
}