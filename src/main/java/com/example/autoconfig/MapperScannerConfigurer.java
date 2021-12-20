package com.example.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.List;

@Slf4j
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor {
	private String basePackage;
	private boolean addToConfig = true;
	private String sqlSessionFactoryBeanName , sqlSessionTemplateBeanName;
	private Class<? extends Annotation> annotationClass;
	private Class<?> markerInterface;
	private Class<? extends MapperFactoryBean<?>> mapperFactoryBeanClass;


	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	public void setAddToConfig(boolean addToConfig) {
		this.addToConfig = addToConfig;
	}


	public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
		this.annotationClass = annotationClass;
	}

	public void setMarkerInterface(Class<?> superClass) {
		this.markerInterface = superClass;
	}


	public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
		this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
	}

	public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
		this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
	}

	public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean<?>> mapperFactoryBeanClass) {
		this.mapperFactoryBeanClass = mapperFactoryBeanClass;
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

		ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
		scanner.setAddToConfig(this.addToConfig);
		scanner.setAnnotationClass(this.annotationClass);
		scanner.setMarkerInterface(this.markerInterface);
		scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
		scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
		scanner.registerFilters();
		String[] basePackages = StringUtils.tokenizeToStringArray(this.basePackage, ",; \t\n");
		log.info("going to scan " + String.join(",", List.of(basePackages)));
		int scan = scanner.scan(basePackages);
		log.info("scan result: " + scan);
	}

}
