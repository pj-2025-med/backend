package com.example.med.config;

import com.example.med.mapper.StudyCommentMapper;
import com.example.med.mapper.second.AnnotationMapper;
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

        // MyBatis 설정 추가
        org.apache.ibatis.session.Configuration mybatisConfiguration = new org.apache.ibatis.session.Configuration();
        mybatisConfiguration.setMapUnderscoreToCamelCase(true); // 스네이크 케이스 -> 카멜 케이스 자동 매핑 활성화
        sessionFactory.setConfiguration(mybatisConfiguration);

        return sessionFactory.getObject();
    }

    @Bean
    public PlatformTransactionManager secondTransactionManager(@Qualifier("secondDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // UserInfoMapper를 위한 MapperFactoryBean을 수동으로 등록합니다.
    @Bean
    public MapperFactoryBean<UserInfoMapper> userInfoMapper(@Qualifier("secondSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<UserInfoMapper> factoryBean = new MapperFactoryBean<>(UserInfoMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    // StudyCommentMapper를 위한 MapperFactoryBean을 수동으로 등록합니다.
    @Bean
    public MapperFactoryBean<StudyCommentMapper> studyCommentMapper(@Qualifier("secondSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<StudyCommentMapper> factoryBean = new MapperFactoryBean<>(StudyCommentMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }

    // AnnotationMapper를 위한 MapperFactoryBean을 수동으로 등록합니다.
    @Bean
    public MapperFactoryBean<AnnotationMapper> annotationMapper(@Qualifier("secondSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<AnnotationMapper> factoryBean = new MapperFactoryBean<>(AnnotationMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }
}
