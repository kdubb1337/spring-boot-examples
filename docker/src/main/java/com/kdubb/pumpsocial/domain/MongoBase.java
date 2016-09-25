package com.kdubb.pumpsocial.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kdubb.pumpsocial.ObjectIdJsonSerializer;

@Document
public class MongoBase {

	@Id
	@JsonSerialize(using=ObjectIdJsonSerializer.class)
	private ObjectId id;

	public MongoBase() {
		id = new ObjectId();
	}
	
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}
}