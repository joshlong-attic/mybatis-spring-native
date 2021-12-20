package org.mybatis.spring.autoconfigure;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
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
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

@Slf4j
class SimplifiedAutoConfiguredMapperRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

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
		Collection<String> basePackages = this.getBasePackages();
		ClassPathScanningCandidateComponentProvider scanner = this.buildScanner();
		basePackages.forEach(basePackage -> scanner.findCandidateComponents(basePackage)
			.stream()
			.filter(cc -> cc instanceof AnnotatedBeanDefinition)
			.forEach(beanDefinition -> {
				AnnotationMetadata metadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
				boolean isMapper = metadata.getAnnotationTypes().stream().anyMatch(s -> s.equals(Mapper.class.getName()));
				if (isMapper) {
					registerMapper(metadata, registry);
				}
			}));
	}


	private String getBeanNameForType(Class<?> clazz, ListableBeanFactory factory) {
		String[] beanNamesForType = factory.getBeanNamesForType(clazz);
		if (beanNamesForType.length > 0) {
			return beanNamesForType[0];
		}
		return null;
	}

	@SneakyThrows
	private void registerMapper(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		Class<MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;
		Class<?> mapperClazzName = Class.forName(metadata.getClassName());
		String className = metadata.getClassName();
		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(mapperFactoryBeanClass);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		definition.addPropertyValue("mapperInterface", mapperClazzName);
		if (this.factory instanceof ListableBeanFactory) {
			ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.factory;
			Map<String, Class<?>> types = Map.of("sqlSessionFactory", SqlSessionFactory.class, "sqlSessionTemplate", SqlSessionTemplate.class);
			types.forEach((property, type) -> {
				String beanName = getBeanNameForType(type, listableBeanFactory);
				if (StringUtils.hasText(beanName)) {
					definition.addPropertyValue(property, new RuntimeBeanReference(beanName));
				}
			});
		}
		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
		beanDefinition.setPrimary(true);
		beanDefinition.setBeanClass(mapperFactoryBeanClass);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[0]);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private Collection<String> getBasePackages() {
		return AutoConfigurationPackages.get(this.factory);
	}
}
