package com.example.mybatisnative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.beans.FeatureDescriptor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.nativex.hint.TypeAccess.*;


//@EnableConfigurationProperties(MybatisProperties.class)
@TypeHint(
	types = {City.class, CityMapper.class},
	access = {
		PUBLIC_CONSTRUCTORS,
		PUBLIC_CLASSES,
		PUBLIC_FIELDS,
		PUBLIC_METHODS,
		DECLARED_CLASSES,
		DECLARED_CONSTRUCTORS,
		DECLARED_FIELDS,
		DECLARED_METHODS
	}
)
@JdkProxyHint(types = CityMapper.class)
@SpringBootApplication(exclude = {
	MybatisLanguageDriverAutoConfiguration.class,
	MybatisAutoConfiguration.class
})
public class MybatisNativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MybatisNativeApplication.class, args);
	}

/*
	@Bean
	SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory);
	}

	@Bean
	CityMapper cityMapper(SqlSessionTemplate sqlSessionTemplate) {
		return sqlSessionTemplate.getMapper(CityMapper.class);
	}

	@Bean
	SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		Configuration configuration = new Configuration();
		configuration.addMapper(CityMapper.class);
		factoryBean.setConfiguration(configuration);
		return factoryBean;
	}*/

	@Bean
	ApplicationRunner runner(CityMapper cityMapper) {
		return args -> {

			for (var c : cityMapper.getClass().getInterfaces())
				System.out.println("class: " + c.getName());

			cityMapper.insert(new City(null, "NYC", "NY", "USA"));
			cityMapper.findAll().forEach(System.out::println);
		};
	}
}

