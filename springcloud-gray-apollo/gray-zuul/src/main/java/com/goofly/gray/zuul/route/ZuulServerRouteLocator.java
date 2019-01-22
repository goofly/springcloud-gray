package com.goofly.gray.zuul.route;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

import com.goofly.gray.zuul.apollo.UrlMapConfigProp;


public class ZuulServerRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {
    @Autowired
    private UrlMapConfigProp urlMapConfigProp;
    
   public ZuulServerRouteLocator(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
    }

    @Override
    public void refresh() {
        doRefresh();
    }
    //覆盖这个方法，从重实现它
    @Override
    protected Map<String, ZuulProperties.ZuulRoute> locateRoutes() {
    	
        //重新定义一个路由映射map
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();
        //把父类中的映射继承下来，它主要是从配置文件中取的映射。
        routesMap.putAll(super.locateRoutes());
        //这里的路由信息来自于配置文件
        Set<Entry<String, String>> entrySet = urlMapConfigProp.getMap().entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            String serverId = entry.getKey();
            String path = entry.getValue().toLowerCase();
            
            ZuulRoute zuulRoute = new ZuulRoute();
            zuulRoute.setServiceId(serverId);
            zuulRoute.setPath(path);
            zuulRoute.setId(StringUtils.substringBeforeLast(path, "/"));
            routesMap.put(path, zuulRoute);
        }return routesMap;
    }

}