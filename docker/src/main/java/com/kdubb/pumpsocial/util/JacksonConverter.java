package com.kdubb.pumpsocial.util;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonConverter implements Converter {
	private final ObjectMapper objectMapper;

	public JacksonConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public Object fromBody(TypedInput body, Type type) throws ConversionException {
		JavaType javaType = objectMapper.getTypeFactory().constructType(type);
		
		try {
			if(body == null || body.in() == null || body.in().available() == 0)
				return null;
			
			return objectMapper.readValue(body.in(), javaType);
		}
		catch (IOException e) {
			throw new ConversionException(e);
		}
	}

	@Override
	public TypedOutput toBody(Object object) {
		try {
			String charset = "UTF-8";
			return new JsonTypedOutput(objectMapper.writeValueAsString(object).getBytes(charset), charset);
		}
		catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static class JsonTypedOutput implements TypedOutput {
		private final byte[] jsonBytes;
		private final String mimeType;
		
		public JsonTypedOutput(byte[] jsonBytes, String charset) {
			this.jsonBytes = jsonBytes;
			this.mimeType = "application/json; charset=" + charset;
		}

		@Override
		public String fileName() {
			return null;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}

		@Override
		public long length() {
			return jsonBytes.length;
		}

		@Override
		public void writeTo(OutputStream out) throws IOException {
			out.write(jsonBytes);
		}
	}
}