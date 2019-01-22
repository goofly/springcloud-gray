package com.goofly.gray.zuul.filter;

import com.goofly.gray.core.CoreHeaderInterceptor;
import com.netflix.zuul.ZuulFilter;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

public class GrayPostZuulFilter extends ZuulFilter {
	@Override
	public String filterType() {
		return FilterConstants.POST_TYPE;
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		CoreHeaderInterceptor.shutdownHystrixRequestContext();
		return null;
	}
}
