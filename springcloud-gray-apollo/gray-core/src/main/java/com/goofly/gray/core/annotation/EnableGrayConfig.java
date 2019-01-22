package com.goofly.gray.core.annotation;

import com.goofly.gray.apollo.ApolloConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ApolloConfig.class)
@Documented
@Inherited
public @interface EnableGrayConfig {

}