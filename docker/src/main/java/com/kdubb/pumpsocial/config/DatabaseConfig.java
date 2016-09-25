package com.kdubb.pumpsocial.config;

//import com.mchange.v2.c3p0.ComboPooledDataSource;
//
//@Configuration
public class DatabaseConfig {
//	@Value(ConfigConstants.DB_DRIVER)
//	private String dbDriver;
//
//	@Value(ConfigConstants.DB_URL)
//	private String dbUrl;
//
//	@Value(ConfigConstants.DB_SCHEMA)
//	private String dbSchema;
//
//	@Value(ConfigConstants.DB_USERNAME)
//	private String dbUsername;
//
//	@Value(ConfigConstants.DB_PASSWORD)
//	private String dbPassword;
//
//	@Value(ConfigConstants.DB_MIN_SIZE)
//	private int dbMinSize;
//
//	@Value(ConfigConstants.DB_MAX_SIZE)
//	private int dbMaxSize;
//
//	@Value(ConfigConstants.DB_MAX_STATEMENTS)
//	private int dbMaxStatements;
//
//	@Value(ConfigConstants.DB_TEST_PERIOD)
//	private int dbTestPeriod;
//
//	@Value(ConfigConstants.DB_MAX_IDLE)
//	private int dbMaxIdle;
//
//	@Value(ConfigConstants.DB_ACQUIRE_INCREMENT)
//	private int dbAcquireIncrement;
//
//	@Value(ConfigConstants.DB_UNRETURNED_CONNECTION_TIMEOUT)
//	private int dbUnreturnedConnectionTimeout;
//
//	@Value(ConfigConstants.JPA_DDL)
//	private String jpaDdl;
//
//	@Value(ConfigConstants.JPA_DRIVER)
//	private String jpaDriver;
//
//	@Value(ConfigConstants.JPA_LOG_SQL)
//	private boolean jpaLogSql;
//
//	private static final Logger LOG = LogManager.getLogger(DatabaseConfig.class);
//
//	@Bean
//	public DataSource dataSource() {
//		ComboPooledDataSource dataSource = new ComboPooledDataSource();
//
//		try {
//			dataSource.setDriverClass(dbDriver);
//		}
//		catch (PropertyVetoException e) {
//			LOG.error("Failed to set DB Driver", e);
//		}
//
//		dataSource.setJdbcUrl(dbUrl + dbSchema);
//		dataSource.setUser(dbUsername);
//		dataSource.setPassword(dbPassword);
//		dataSource.setMinPoolSize(dbMinSize);
//		dataSource.setMaxPoolSize(dbMaxSize);
//		dataSource.setMaxStatements(dbMaxStatements);
//		dataSource.setIdleConnectionTestPeriod(dbTestPeriod);
//		dataSource.setMaxIdleTime(dbMaxIdle);
//		dataSource.setAcquireIncrement(dbAcquireIncrement);
//		dataSource.setUnreturnedConnectionTimeout(dbUnreturnedConnectionTimeout);
//		dataSource.setPreferredTestQuery("SELECT 1");
//
//		return dataSource;
//	}
//	
//	@Bean
//	public PlatformTransactionManager transactionManager() {
//		return new DataSourceTransactionManager(dataSource());
//	}
//	
//	@Bean
//	public JdbcTemplate jdbcTemplate() {
//		return new JdbcTemplate(dataSource());
//	}
}