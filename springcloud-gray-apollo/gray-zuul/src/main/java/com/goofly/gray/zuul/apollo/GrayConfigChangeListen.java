package com.goofly.gray.zuul.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.goofly.gray.vo.GrayInfoVO;
import com.goofly.gray.zuul.route.RefreshRouteService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScope;

/**
 * 使用@ApolloConfig自动注入Config对象
 * 使用@ApolloConfigChangeListener自动注入ConfigChangeListener对象 当监听到属性值发生变化后使用Config
 * API修改属性值
 */
public class GrayConfigChangeListen{
	
	@Autowired
	private RefreshScope refreshScope;
	
    @Autowired
    RefreshRouteService refreshRouteService;

	@ApolloConfig
	private Config config;
	
	private static final Logger logger = LoggerFactory.getLogger(GrayConfigChangeListen.class);

	@ApolloConfigChangeListener("application")
	private void routeChange(ConfigChangeEvent changeEvent) {
		changeEvent.changedKeys().forEach(key -> {
			ConfigChange change = changeEvent.getChange(key);
			logger.info("=====>routeChange,  key: {}, oldValue: {}, newValue: {}, changeType: {}",change.getPropertyName(), change.getOldValue(), change.getNewValue(), change.getChangeType());
			
			// 动态路由
			if(StringUtils.startsWith(change.getPropertyName(), GrayInfoVO.urlMap)) {
				refreshScope.refreshAll();
				refreshRouteService.refreshRoute();
			}

		});
		
	}
	
	@ApolloConfigChangeListener("platform.grayuser")
	private void gray(ConfigChangeEvent changeEvent) {
		changeEvent.changedKeys().forEach(key -> {
			ConfigChange change = changeEvent.getChange(key);
			logger.info("=========>灰度用户已更新 - key: {}, oldValue: {}, newValue: {}, changeType: {}",change.getPropertyName(), change.getOldValue(), change.getNewValue(), change.getChangeType());
			refreshScope.refresh("grayUserConfigProp");
		});
		
	}
}