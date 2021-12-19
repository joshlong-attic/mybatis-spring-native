package com.example.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.beans.FeatureDescriptor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter({DataSourceAutoConfiguration.class, MybatisLanguageDriverAutoConfiguration.class})
@org.springframework.context.annotation.Configuration
class MyMybatisAutoConfiguration
	implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(MybatisAutoConfiguration.class);
	//	private final MybatisProperties properties;
	private final Interceptor[] interceptors;
	private final TypeHandler[] typeHandlers;
	private final LanguageDriver[] languageDrivers;
	private final ResourceLoader resourceLoader;
	private final DatabaseIdProvider databaseIdProvider;
	private final List<ConfigurationCustomizer> configurationCustomizers;

	MyMybatisAutoConfiguration(ObjectProvider<Interceptor[]> interceptorsProvider, ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<LanguageDriver[]> languageDriversProvider, ResourceLoader resourceLoader, ObjectProvider<DatabaseIdProvider> databaseIdProvider, ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider) {
		this.interceptors = interceptorsProvider.getIfAvailable();
		this.typeHandlers = typeHandlersProvider.getIfAvailable();
		this.languageDrivers = languageDriversProvider.getIfAvailable();
		this.resourceLoader = resourceLoader;
		this.databaseIdProvider = databaseIdProvider.getIfAvailable();
		this.configurationCustomizers = (List) configurationCustomizersProvider.getIfAvailable();
	}

	public void afterPropertiesSet() {
		this.checkConfigFileExists();
	}

	private void checkConfigFileExists() {


	}

	@Bean
	@ConditionalOnMissingBean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		factory.setVfs(SpringBootVFS.class);


		this.applyConfiguration(factory);


		if (!ObjectUtils.isEmpty(this.interceptors)) {
			factory.setPlugins(this.interceptors);
		}

		if (this.databaseIdProvider != null) {
			factory.setDatabaseIdProvider(this.databaseIdProvider);
		}


		if (!ObjectUtils.isEmpty(this.typeHandlers)) {
			factory.setTypeHandlers(this.typeHandlers);
		}

		return factory.getObject();
	}

	private void applyConfiguration(SqlSessionFactoryBean factory) {
		var config = new Configuration();
		if (this.configurationCustomizers != null)	{
			this.configurationCustomizers.forEach(cc -> cc.customize(config));
		}
		factory.setConfiguration(config);
	}

	@Bean
	@ConditionalOnMissingBean
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}

	@org.springframework.context.annotation.Configuration
	@Import({AutoConfiguredMapperScannerRegistrar.class})
	@ConditionalOnMissingBean({MapperFactoryBean.class, MapperScannerConfigurer.class})
	public static class MapperScannerRegistrarNotFoundConfiguration implements InitializingBean {

		public void afterPropertiesSet() {
			logger.debug("Not found configuration for registering mapper bean using @MapperScan, MapperFactoryBean and MapperScannerConfigurer.");
		}
	}

	public static class AutoConfiguredMapperScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar {
		private BeanFactory beanFactory;

		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
			if (!AutoConfigurationPackages.has(this.beanFactory)) {
				logger.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.");
			}
			else {
				logger.debug("Searching for mappers annotated with @Mapper");
				List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
				if (logger.isDebugEnabled()) {
					packages.forEach((pkg) -> {
						logger.debug("Using auto-configuration base package '{}'", pkg);
					});
				}

				BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
				builder.addPropertyValue("processPropertyPlaceHolders", true);
				builder.addPropertyValue("annotationClass", Mapper.class);
				builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));
				BeanWrapper beanWrapper = new BeanWrapperImpl(MapperScannerConfigurer.class);
				Set<String> propertyNames = (Set) Stream.of(beanWrapper.getPropertyDescriptors()).map(FeatureDescriptor::getName).collect(Collectors.toSet());
				if (propertyNames.contains("lazyInitialization")) {
					builder.addPropertyValue("lazyInitialization", "false");
				}

				if (propertyNames.contains("defaultScope")) {
					builder.addPropertyValue("defaultScope", "");
				}

				registry.registerBeanDefinition(MapperScannerConfigurer.class.getName(), builder.getBeanDefinition());
			}
		}

		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}
	}

}
