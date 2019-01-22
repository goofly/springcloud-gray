package com.goofly.gray.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableDefault;

public class CoreHeaderInterceptor extends HandlerInterceptorAdapter {
	private static final Logger logger = LoggerFactory.getLogger(CoreHeaderInterceptor.class);

	public static final String HEADER_VERSION = "version";

	public static final HystrixRequestVariableDefault<String> version = new HystrixRequestVariableDefault<>();

	public static void initHystrixRequestContext(String headerVer) {
		logger.info("initHystrixRequestContext headerVer:{}", headerVer);
		if (!HystrixRequestContext.isCurrentThreadInitialized()) {
			HystrixRequestContext.initializeContext();
		}

		if (!StringUtils.isEmpty(headerVer)) {
			CoreHeaderInterceptor.version.set(headerVer);
		} else {
			CoreHeaderInterceptor.version.set("");
		}
	}

	public static void shutdownHystrixRequestContext() {
		if (HystrixRequestContext.isCurrentThreadInitialized()) {
			HystrixRequestContext.getContextForCurrentThread().shutdown();
		}
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		CoreHeaderInterceptor.initHystrixRequestContext(request.getHeader(CoreHeaderInterceptor.HEADER_VERSION));
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		CoreHeaderInterceptor.shutdownHystrixRequestContext();
	}
}
