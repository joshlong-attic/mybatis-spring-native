package com.example.mybatisnative;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.nativex.hint.TypeHint;

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

