package com.kdubb.pumpsocial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile({ "DEV", "PROD", "quantum1" })
@EnableScheduling
public class ScheduleConfig {

}