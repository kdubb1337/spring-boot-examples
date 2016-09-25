package com.kdubb.pumpsocial.domain;

import java.util.Date;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class CrawledPage extends MongoBase {
	
	@Indexed
	private String link;
	private boolean errored = false;
	private Date time;

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public boolean isErrored() {
		return errored;
	}

	public void setErrored(boolean errored) {
		this.errored = errored;
	}

	public Date getTime() {
		return time;
	}

	public void setTimestamp(Date time) {
		this.time = time;
	}
}