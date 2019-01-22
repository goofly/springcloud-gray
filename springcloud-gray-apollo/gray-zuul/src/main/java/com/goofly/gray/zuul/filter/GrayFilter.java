package com.goofly.gray.zuul.filter;

import com.goofly.gray.core.CoreHeaderInterceptor;
import com.goofly.gray.zuul.apollo.GrayUserConfigProp;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class GrayFilter extends ZuulFilter {

	@Autowired
	private GrayUserConfigProp grayUserConfigProp;

	private static final String HEADER_TOKEN = "token";
	private static final Logger logger = LoggerFactory.getLogger(GrayFilter.class);

	@Override
	public String filterType() {
		return FilterConstants.PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return 1000;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		String token = ctx.getRequest().getHeader(HEADER_TOKEN);

		//String userId = conver2User(token);
		String userId = token;
		log.info("======>userId:{}", userId);

		List<String> userIdList = grayUserConfigProp.getUserIdList();
		String version = userIdList.contains(userId) ? grayUserConfigProp.getVersion() : null;
		logger.info("=====>userId:{},version:{}", userId, version);

		// zuul本身调用微服务
		CoreHeaderInterceptor.initHystrixRequestContext(version);
		// 传递给后续微服务
		ctx.addZuulRequestHeader(CoreHeaderInterceptor.HEADER_VERSION, version);

		return null;
	}

	/**
	 * 获取用户token
	 */
	private String conver2User(String token) {
		if(StringUtils.isEmpty(token) || !token.contains(".")) {
			return null;
		}
		String header = token.split(".")[0];
		return new String(Base64.decode(header));
	}
}
