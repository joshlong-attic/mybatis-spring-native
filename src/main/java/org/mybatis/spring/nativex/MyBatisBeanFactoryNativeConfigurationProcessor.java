package org.mybatis.spring.nativex;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeProxyEntry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
	* Finds and registers reflection hints for all the MyBatis-annotated mappers in the beanFactory
	*
	* @author Josh Long
	*/
@Slf4j
public class MyBatisBeanFactoryNativeConfigurationProcessor implements BeanFactoryNativeConfigurationProcessor {

	@Override
	@SneakyThrows
	public void process(ConfigurableListableBeanFactory beanFactory, NativeConfigurationRegistry registry) {
		TypeAccess[] values = TypeAccess.values();
		String[] beanNamesForAnnotation = beanFactory.getBeanNamesForAnnotation(Mapper.class);
		for (String beanName : beanNamesForAnnotation) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			PropertyValue getMapperInterface = beanDefinition.getPropertyValues().getPropertyValue("mapperInterface");
			if (getMapperInterface.getValue() != null) {
				log.info("got to the point where we're looking up the mapperInterface");
				Class<?> interfaceForMapper = (Class<?>) getMapperInterface.getValue();
				log.info("got to the point where we're looking up the mapperInterface, which is " + interfaceForMapper.getName());
				registry.reflection().forType(interfaceForMapper).withAccess(TypeAccess.values()).build();
				registry.proxy().add(NativeProxyEntry.ofInterfaces(interfaceForMapper));
				log.info("mapper class: " + interfaceForMapper.getName());
				registerMapperRelationships(values, interfaceForMapper, registry);
			}
		}
	}

	private void registerMapperRelationships(TypeAccess[] typeAccesses, Class<?> clzz, NativeConfigurationRegistry registry) {
		log.info("getting the declared methods");
		Method[] methods = ReflectionUtils.getDeclaredMethods(clzz);
		for (Method m : methods) {
			ReflectionUtils.makeAccessible(m);
			registry.reflection().forType(m.getReturnType()).withAccess(typeAccesses).build();
			for (Class<?> parameterClass : m.getParameterTypes()) {
				registry.reflection().forType(parameterClass).withAccess(typeAccesses).build();
			}
		}
	}
}
