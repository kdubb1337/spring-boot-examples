package com.kdubb.pumpsocial;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ObjectIdJsonSerializer extends JsonSerializer<ObjectId> {
	@Override
	public void serialize(ObjectId o, JsonGenerator j, SerializerProvider s) throws IOException, JsonProcessingException {
		if (o == null) {
			j.writeNull();
		}
		else {
			j.writeString(o.toString());
		}
	}
}