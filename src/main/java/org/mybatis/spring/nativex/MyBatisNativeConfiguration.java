package org.mybatis.spring.nativex;


import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.javassist.util.proxy.RuntimeSupport;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.nativex.hint.InitializationHint;
import org.springframework.nativex.hint.InitializationTime;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

import static org.springframework.nativex.hint.TypeAccess.*;


/**
	* Registers hints to make a MyBatis Mapper work in a Spring Native context
	*
	* @author Josh Long
	*/
@NativeHint(
	initialization = {
		@InitializationHint(initTime = InitializationTime.BUILD, types = org.apache.ibatis.type.JdbcType.class)
	},
	options = {"--initialize-at-build-time=org.apache.ibatis.type.JdbcType"}
)
@TypeHint(
	types = {
		com.example.autoconfig.MapperScannerConfigurer.class,// Josh's cleaned up version for debugging
		MapperScannerConfigurer.class,
		//		something's wrong im being sent on a wild goose chase above ignore the stuff above this
		RawLanguageDriver.class,
		XMLLanguageDriver.class,
		RuntimeSupport.class,
		ProxyFactory.class,
		Slf4jImpl.class,
		Log.class,
		JakartaCommonsLoggingImpl.class,
		Log4jImpl.class,
		Log4j2Impl.class,
		Jdk14LoggingImpl.class,
		StdOutImpl.class,
		NoLoggingImpl.class,
		SqlSessionFactory.class,
		SqlSessionFactoryBean.class,
	}, //
	access = {
		PUBLIC_CONSTRUCTORS,
		PUBLIC_CLASSES,
		PUBLIC_FIELDS,
		PUBLIC_METHODS,
		DECLARED_CLASSES,
		DECLARED_CONSTRUCTORS,
		DECLARED_FIELDS,
		DECLARED_METHODS
	})

public class MyBatisNativeConfiguration
	implements NativeConfiguration {

}
