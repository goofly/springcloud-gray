package com.goofly.gray.apollo;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.goofly.gray.vo.GrayInfoVO;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.netflix.appinfo.EurekaInstanceConfig;

/**
 * 使用@ApolloConfig自动注入Config对象
 * 使用@ApolloConfigChangeListener自动注入ConfigChangeListener对象 当监听到属性值发生变化后使用Config
 * API修改属性值
 */
@EnableApolloConfig
public class ConfigChangeListen{

	@ApolloConfig
	private Config config;
	
    @Autowired
    private EurekaInstanceConfig eurekaInstanceConfig;
    
    @Value("${eureka.client.serviceUrl.defaultZone:}")
    private String eurekaUrl;
    
    @Value("${eureka.client.service-url.defaultZone:}")
    private String eureka_url;
    
    private String connUrl;
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigChangeListen.class);

	@ApolloConfigChangeListener("application")
	private void someOnChange(ConfigChangeEvent changeEvent) {
		changeEvent.changedKeys().forEach(key -> {
			ConfigChange change = changeEvent.getChange(key);
			logger.debug("Found change - key: {}, oldValue: {}, newValue: {}, changeType: {}",change.getPropertyName(), change.getOldValue(), change.getNewValue(), change.getChangeType());
			
			// 灰度配置
			if(GrayInfoVO.version.equals(change.getPropertyName())) {
				String appname = eurekaInstanceConfig.getAppname();
				String instanceId = eurekaInstanceConfig.getInstanceId();
				String value = StringUtils.isEmpty(change.getNewValue()) ? "" : change.getNewValue();
				
	            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
	            	HttpPut httpPut = new HttpPut(connUrl+"apps/"+appname+"/"+instanceId+"/metadata?version="+value);
	                //httpPut.setEntity(new StringEntity("version, v1"));
	            	
	            	logger.debug("======>请求url:{}",httpPut.getURI());
	            	
	                ResponseHandler<String> responseHandler = response -> {
	                    int status = response.getStatusLine().getStatusCode();
	                    if (status >= 200 && status < 300) {
	                        HttpEntity entity = response.getEntity();
	                        return entity != null ? EntityUtils.toString(entity) : null;
	                    } else {
	                        throw new ClientProtocolException("Unexpected response status: " + status);
	                    }
	                };
	                String responseBody = httpclient.execute(httpPut, responseHandler);
	                logger.debug("======返回结果:{}",responseBody);
	            } catch (IOException e) {
					e.printStackTrace();
				}
			}

		});
	}
	

    @PostConstruct
    public void initConfig() {
    	this.connUrl = StringUtils.isEmpty(eurekaUrl) ? eureka_url : eurekaUrl;
    	logger.info("===>connUrl:{}",connUrl);
    }
}