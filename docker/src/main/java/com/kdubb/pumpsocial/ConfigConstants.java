package com.kdubb.pumpsocial;

public class ConfigConstants {

	// Database
	public static final String DB_DRIVER = "${db.driver}";
	public static final String DB_URL = "${db.url}";
	public static final String DB_SCHEMA = "${db.schema}";
	public static final String DB_USERNAME = "${db.username}";
	public static final String DB_PASSWORD = "${db.password}";
	public static final String DB_MIN_SIZE = "${db.minSize}";
	public static final String DB_MAX_SIZE = "${db.maxSize}";
	public static final String DB_MAX_STATEMENTS = "${db.maxStatements}";
	public static final String DB_TEST_PERIOD = "${db.testPeriod}";
	public static final String DB_MAX_IDLE = "${db.maxIdle}";
	public static final String DB_ACQUIRE_INCREMENT = "${db.acquireIncrement}";
	public static final String DB_UNRETURNED_CONNECTION_TIMEOUT = "${db.unreturnedConnectionTimeout}";

	public static final String JPA_DDL = "${jpa.ddl}";
	public static final String JPA_DRIVER = "${jpa.driver}";
	public static final String JPA_LOG_SQL = "${jpa.log.sql}";

	// Thread Pool
	public static final String POOL_MAX_SIZE = "${pool.maxSize}";
	public static final String POOL_QUEUE_SIZE = "${pool.queueSize}";

	// MongoDB
	public static final String MONGO_DB_HOST = "${mongo.db.host}";
	public static final String MONGO_DB_PORT = "${mongo.db.port}";
	public static final String MONGO_DB_NAME = "${mongo.db.name}";
	public static final String MONGO_DB_USERNAME = "${mongo.db.username}";
	public static final String MONGO_DB_PASSWORD = "${mongo.db.password}";

	public static final String MONGO_DB2_HOST = "${mongo.db2.host}";
	public static final String MONGO_DB2_PORT = "${mongo.db2.port}";
	public static final String MONGO_DB2_NAME = "${mongo.db2.name}";
	public static final String MONGO_DB2_USERNAME = "${mongo.db2.username}";
	public static final String MONGO_DB2_PASSWORD = "${mongo.db2.password}";

	// Social
	public static final String FB_KEY = "${facebook.appKey}";
	public static final String FB_SECRET = "${facebook.appSecret}";
	public static final String FB_NAMESPACE = "${facebook.appNamespace}";
	public static final String TWITTER_KEY = "${twitter.appKey}";
	public static final String TWITTER_SECRET = "${twitter.appSecret}";
	public static final String LINKEDIN_KEY = "${linkedin.appKey}";
	public static final String LINKEDIN_SECRET = "${linkedin.appSecret}";
	public static final String TUMBLR_KEY = "${tumblr.appKey}";
	public static final String TUMBLR_SECRET = "${tumblr.appSecret}";
	public static final String GOOGLE_KEY = "${google.appKey}";
	public static final String GOOGLE_SECRET = "${google.appSecret}";
	public static final String INSTAGRAM_KEY = "${instagram.appKey}";
	public static final String INSTAGRAM_SECRET = "${instagram.appSecret}";
	public static final String SES_ACCESS_KEY = "${ses.appKey}";
	public static final String SES_SECRET_KEY = "${ses.appSecret}";
}