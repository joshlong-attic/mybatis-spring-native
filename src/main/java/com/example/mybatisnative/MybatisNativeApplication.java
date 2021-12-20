package com.example.mybatisnative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;

/**
	* Demonstrates a trivial autoconfiguration supporting MyBatis mappers in a Spring Native context.
	*
	* @author Josh Long
	*/
@Slf4j
@SpringBootApplication(exclude = {
	MybatisLanguageDriverAutoConfiguration.class,
	MybatisAutoConfiguration.class
})
public class MybatisNativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MybatisNativeApplication.class, args);
	}

	@Bean
	ApplicationRunner runner(CityMapper cityMapper) {
		return args -> {
			cityMapper.insert(new City(null, "NYC", "NY", "USA"));
			cityMapper.findAll().forEach(System.out::println);
		};
	}
}

