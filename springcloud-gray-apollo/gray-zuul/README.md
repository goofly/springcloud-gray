# 使用

**配置示例**

```java
url.map.provide-test = /pt/**
```

`url.map.`为固定写法，

`provide-test`为服务名称，

`/pt/**`为映射路径





# 设计

## 前言



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



## 动态路由

[参考文章](https://blog.csdn.net/u013815546/article/details/68944039)