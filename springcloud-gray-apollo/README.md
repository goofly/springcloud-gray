[代码git地址](https://github.com/goofly/springcloud-gray/tree/master/springcloud-gray-apollo)

## 前言

   [上篇文章](https://segmentfault.com/a/1190000017412946)介绍了SpringCloud灰度的实现及流程，每次修改服务的元数据信息`metadata-map`值需要重新调用一次eureka的RestFul接口，不仅如此当服务重启后又会重新读最初的配置值，这样不仅麻烦而且还不可靠。

   在经过与SpringCloud Config 、Disconf、Apollo等配置中心作出对比后，发现被Apollo友好方便的管理端所深深吸引，再加上该配置中心支持配置文件的灰度发布简直不要太完美。



### Apollo灰度配置

让多个实例共享一个配置文件，示例配置

```java
spring.application.name = provide-test
server.port = 7770
eureka.client.service-url.defaultZone = http://localhost:1111/eureka/
```

然后新起一个灰度配置,让对应的服务使用该配置。

```java
eureka.instance.metadata-map.version = v1
```

![主版本](https://github.com/goofly/file/blob/master/SpringCloud-xli/%E4%B8%BB%E7%89%88%E6%9C%AC.png?raw=true)

![](https://github.com/goofly/file/blob/master/SpringCloud-xli/%E7%81%B0%E5%BA%A6%E7%89%88%E6%9C%AC.png?raw=true)



## 事件监听

   监听Apollo事件，当发现配置文件中的`eureka.instance.metadata-map.version`值若发生改变，则调用eureka接口更改`metadata-map`元数据

```
	@ApolloConfigChangeListener("application")
	private void someOnChange(ConfigChangeEvent changeEvent) {
		changeEvent.changedKeys().forEach(key -> {
			ConfigChange change = changeEvent.getChange(key);
			// 灰度配置
			if("eureka.instance.metadata-map.version".equals(change.getPropertyName())) {
				String appname = eurekaInstanceConfig.getAppname();
				String instanceId = eurekaInstanceConfig.getInstanceId();
				String value = StringUtils.isEmpty(change.getNewValue()) ? "" : change.getNewValue();
			
			//TODO 调用eureka更改元数据接口
			}

		});
	}
```



## 灰度使用

在启动类添加注解

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableGrayConfig
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```



