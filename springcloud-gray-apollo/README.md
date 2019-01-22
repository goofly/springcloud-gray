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

这样一来，只需要通过修改配置文件然后就会触发监听事件从而自动触发请求eureka更改元数据的值。



## 动态路由

在通过zuul调用服务时一般有两种方式

**方式1：**

​    在通过zuul调用服务时，加上服务名称后直接调用该服务的接口即可。但是这样的话可能就对外暴露了我们的服务名称了。

```java
# 8000为zuul端口
http://localhost:8000/provider-test/user/getId?id=111
```

**方式2：**
通过配置将servicesId和path做一个映射关系，如下

```java
zuul.routes.users.path=/pt/**
zuul.routes.users.serviceId=provider-test
```

​    但是，这样固定的写法带来的弊端也非常明显。如果想要修改path或者说有新的服务想要加入，那意味着得重新启动zuul服务让其生效。

### 动态路由使用

在网关zuul整合了动态路由功能，监听Apollo配置文件使其修改配置文件后可以马上生效。

**配置示例**

```java
url.map.provide-test = /pt/**
```

`url.map.`为固定写法，`provide-test`为服务名称，`/pt/**`为映射路径

[参考文章](https://blog.csdn.net/u013815546/article/details/68944039)

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



