package com.goofly.gray.apollo;

import org.springframework.context.annotation.Bean;


public class ApolloConfig {
	
	@Bean
    public ConfigChangeListen javaConfigSample(){
        return new ConfigChangeListen();
    }
}
