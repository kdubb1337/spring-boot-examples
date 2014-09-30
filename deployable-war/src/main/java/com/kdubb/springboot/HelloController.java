package com.kdubb.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HelloController {
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String index(Model model) {
		// let�s pass some variables to the view script
		model.addAttribute("wisdom", "Goodbye XML");

		// renders /WEB-INF/views/hello.jsp
		return "hello";
	}
}