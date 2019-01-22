package com.goofly.gray.zuul.apollo;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableApolloConfig
public class GrayApolloConfig {

    @Bean
    public GrayConfigChangeListen javaConfigSample(){
        return new GrayConfigChangeListen();
    }
    
    @Bean
    @RefreshScope
    public GrayUserConfigProp grayUserConfigProp() {
    	return new GrayUserConfigProp();
    }
    
    @Bean
    @RefreshScope
    public UrlMapConfigProp urlMapConfigProp() {
    	return new UrlMapConfigProp();
    }
}
