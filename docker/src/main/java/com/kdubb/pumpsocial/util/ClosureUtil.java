
package com.kdubb.pumpsocial.util;

import java.io.Closeable;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ClosureUtil {
	private static final Logger LOG = LogManager.getLogger(ClosureUtil.class);

	public static void close(Closeable closeable) {
		if(closeable != null) {
			try {
				closeable.close();
			}
			catch(IOException e) {
				LOG.error("Failed to close Closeable of type {" + closeable.getClass() + "}", e);
			}
		}
	}
}