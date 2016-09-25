package com.kdubb.pumpsocial.api;

import retrofit.http.GET;
import retrofit.http.Query;

import com.kdubb.pumpsocial.domain.facebook.FacebookWall;

public interface FacebookRssApi {
	
	@GET("/feeds/page.php")
	public FacebookWall getWall(@Query("format") String format, @Query("id") String id);
}