package com.example.autoconfig;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

@Import({AutoConfiguredMapperScannerRegistrar.class})
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnSingleCandidate(DataSource.class)
@AutoConfigureAfter({DataSourceAutoConfiguration.class})
@org.springframework.context.annotation.Configuration
public class MyMybatisAutoConfiguration {

	@Bean
	SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);
		factory.setVfs(SpringBootVFS.class);
		return factory;
	}

	@Bean
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}

@Slf4j
class AutoConfiguredMapperScannerRegistrar
	implements BeanFactoryAware,
	ImportBeanDefinitionRegistrar,
	ResourceLoaderAware, EnvironmentAware {

	private ResourceLoader loader;
	private Environment environment;
	private BeanFactory factory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.loader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	private ClassPathScanningCandidateComponentProvider buildScanner() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false,
			environment) {

			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return beanDefinition.getMetadata().isIndependent();
			}

			@Override
			protected boolean isCandidateComponent(MetadataReader metadataReader) {
				return !metadataReader.getClassMetadata().isAnnotation();
			}
		};
		scanner.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));
		scanner.setResourceLoader(loader);
		return scanner;
	}

	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		List<String> strings = AutoConfigurationPackages.get(this.factory);
		log.info("found the following package: " + String.join(",", strings));

		Collection<String> basePackages = this.getBasePackages(importingClassMetadata);
		ClassPathScanningCandidateComponentProvider scanner = this.buildScanner();
		basePackages.forEach(basePackage -> scanner.findCandidateComponents(basePackage)
			.stream()
			.filter(cc -> cc instanceof AnnotatedBeanDefinition)
			.forEach(beanDefinition -> {
				AnnotationMetadata metadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
				boolean isMapper = metadata.getAnnotationTypes().stream()
					.anyMatch(s -> s.equals(Mapper.class.getName()));
				if (!isMapper) {
					return;
				}
				registerMapper(metadata, registry);
			}));
	}

	@SneakyThrows
	private void registerMapper(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

		String sqlSessionFactoryBeanName = null;
		if (this.factory instanceof ListableBeanFactory lbf) {
			String[] beanNamesForType = lbf.getBeanNamesForType(SqlSessionFactory.class);
			log.info(String.join(",", beanNamesForType));
			Assert.isTrue(beanNamesForType.length > 0, () -> "there must be at least one bean of type " + SqlSessionFactory.class.getName());
			sqlSessionFactoryBeanName = beanNamesForType[0];
		}

	/*	String[] beanDefinitionNames = registry.getBeanDefinitionNames();
		for (String bn : beanDefinitionNames) {
			BeanDefinition bd = registry.getBeanDefinition(bn);
		 if (bn.equalsIgnoreCase("sqlSessionFactory")) {
			  log.info ("found the SQLSessionFactory this one time..");
			}

			log.info(bn + " : " + bd.getBeanClassName() + ":" +
				(bd.getResolvableType().getRawClass() == null ? "" : bd.getResolvableType().getRawClass().getName())
			);
		}*/


		log.info("the factory is " + factory.getClass().getName());


		Class<MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;
		Class<?> mapperClazzName = Class.forName(metadata.getClassName());
		log.info("trying to create a factory bean for " + metadata.getClassName());
		String className = metadata.getClassName();
		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(mapperFactoryBeanClass);
		definition.addPropertyValue("mapperInterface", mapperClazzName);
		if (sqlSessionFactoryBeanName != null)
			definition.addPropertyValue("sqlSessionFactory", new RuntimeBeanReference(sqlSessionFactoryBeanName));
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setPrimary(true);
		beanDefinition.setBeanClass(mapperFactoryBeanClass);
		System.out.println("beanClass: " + mapperFactoryBeanClass.getName());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[0]);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	// todo make this work in a more robust fashion for MyBatis
	private Collection<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		return List.of("com.example.mybatisnative");
	}


}

//


