### feature

- [x] 灰度服务
- [x] 灰度数据
- [x] 灰度管理端
- [x] 动态路由
- [x] 动态数据



### 灰度设计

![灰度设计](https://image-static.segmentfault.com/276/794/2767942178-5c175364d8d13)



1. 用户请求首先到达Nginx然后转发到网关`zuul`，此时`zuul`拦截器会根据用户携带请求`token`解析出对应的`userId`

2. 网关从Apollo配置中心拉取灰度用户列表，然后根据灰度用户策略判断该用户是否是灰度用户。如是，则给该请求添加**请求头**及**线程变量**添加信息`version=xxx`；若不是，则不做任何处理放行

3. 在`zuul`拦截器执行完毕后，`zuul`在进行转发请求时会通过负载均衡器Ribbon。

4. 负载均衡Ribbon被重写。当请求到达时候，Ribbon会取出`zuul`存入**线程变量**值`version`。于此同时，Ribbon还会取出所有缓存的服务列表（定期从eureka刷新获取最新列表）及其该服务的`metadata-map`信息。然后取出服务`metadata-map`的`version`信息与线程变量`version`进行判断对比，若值一直则选择该服务作为返回。若所有服务列表的version信息与之不匹配，则返回null，此时Ribbon选取不到对应的服务则会报错！

5. 当服务为非灰度服务，即没有version信息时，此时Ribbon会收集所有非灰度服务列表，然后利用Ribbon默认的规则从这些非灰度服务列表中返回一个服务。

   ------

6. `zuul`通过Ribbon将请求转发到consumer服务后，可能还会通过`fegin`或`resttemplate`调用其他服务，如provider服务。但是无论是通过`fegin`还是`resttemplate`，他们最后在选取服务转发的时候都会通过`Ribbon`。

7. 那么在通过`fegin`或`resttemplate`调用另外一个服务的时候需要设置一个拦截器，将请求头`version=xxx`给带上，然后存入线程变量。

8. 在经过`fegin`或`resttemplate` 的拦截器后最后会到Ribbon，Ribbon会从**线程变量**里面取出`version`信息。然后重复步骤（4）和（5）

![灰度流程](https://image-static.segmentfault.com/204/546/2045461176-5c185ca2e35d7)



### 灰度使用

引包

```
		<dependency>
			<groupId>com.sygroup</groupId>
			<artifactId>xli-gray-core</artifactId>
			<version>${parent.version}</version>
			<exclusions>
				<exclusion>
					<groupId>com.ctrip.framework.apollo</groupId>
					<artifactId>apollo-client</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
```



配置文件示例

```java
spring.application.name = provide-test
server.port = 7770
eureka.client.service-url.defaultZone = http://localhost:1111/eureka/

#启动后直接将该元数据信息注册到eureka
#eureka.instance.metadata-map.version = v1
```

#### 测试案例

​    分别启动四个测试实例，有version代表灰度服务，无version则为普通服务。当灰度服务测试没问题的时候，通过PUT请求eureka接口将version信息去除，使其变成普通服务.

**实例列表**：

- [x] zuul-server

- [x] provider-test
  `port:7770  version:无`
  `port: 7771  version:v1`

- [x] consumer-test

  `port:8880  version:无`

  `port: 8881  version:v1`

  

#### 修改服务信息

服务在eureka的元数据信息可通过接口http://localhost:1111/eureka/apps访问到。

![](https://github.com/goofly/file/blob/master/SpringCloud-xli/eureka-rest.png?raw=true)

**eureka元数据**:

- 标准元数据：主机名，IP地址，端口号，状态页健康检查等信息
- 自定义元数据：通过`eureka.instance.metadata-map`配置

**更改元数据**:

- 源码地址：`com.netflix.eureka.resources.InstanceResource.updateMetadata()`
- 接口地址： `/eureka/apps/appID/instanceID/metadata?key=value`
- 调用方式：`PUT`

![](https://github.com/goofly/file/blob/master/SpringCloud-xli/postman%E6%9B%B4%E6%94%B9metadata-map.png?raw=true)



**服务信息实例：**

访问接口查看信息http://localhost:1111/eureka/apps/PROVIDE-TEST

![服务info信息](https://image-static.segmentfault.com/330/610/3306102598-5c1895b78c89b)

**注意事项**

> ​    通过此种方法更改server的元数据后，由于ribbon会缓存实力列表，所以在测试改变服务信息时，ribbon并不会立马从eureka拉去最新信息m，这个拉取信息的时间可自行配置。
>
> 同时，当服务重启时服务会重新将配置文件的version信息注册上去。



#### 测试演示

**zuul==>provider服务**

> 用户andy为灰度用户。
> 1.测试灰度用户andy,是否路由到灰度服务`provider-test:7771`
> 2.测试非灰度用户andyaaa(任意用户)是否能被路由到普通服务`provider-test:7770`



![zuul-服务](https://image-static.segmentfault.com/241/097/2410979508-5c18a49d20239)



**zuul==>consumer服务>provider服务**

> 以同样的方式再启动两个consumer-test服务，这里不再截图演示。
>
> 请求从zuul==>consumer-test==>provider-test,通过`fegin`和`resttemplate`两种请求方式测试



Resttemplate请求方式

![zuul-服务-resttemplate服务](https://image-static.segmentfault.com/383/904/3839043359-5c18a61288d2c)

fegin请求方式

![zuul-服务-fegin](https://image-static.segmentfault.com/405/525/405525019-5c18a560541ab)



> 

