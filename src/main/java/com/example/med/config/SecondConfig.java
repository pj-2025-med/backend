package com.example.med.config;

import com.example.med.mapper.second.UserInfoMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean; // MapperFactoryBean 임포트
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class SecondConfig {

    @Bean(name = "secondDataSource")
    @ConfigurationProperties(prefix = "second-datasource")
    public DataSource secondDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public SqlSessionFactory secondSqlSessionFactory(@Qualifier("secondDataSource") DataSource dataSource, ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(applicationContext.getResources("classpath:mapper/second/**/*.xml"));
        sessionFactory.setTypeAliasesPackage("com.example.med.dto");
        return sessionFactory.getObject();
    }

    @Bean
    public PlatformTransactionManager secondTransactionManager(@Qualifier("secondDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // highlight-start
    // UserInfoMapper를 위한 MapperFactoryBean을 수동으로 등록합니다.
    @Bean
    public MapperFactoryBean<UserInfoMapper> userInfoMapper(@Qualifier("secondSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<UserInfoMapper> factoryBean = new MapperFactoryBean<>(UserInfoMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }
    // highlight-end
}