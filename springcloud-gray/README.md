[代码GIT](https://github.com/goofly/springcloud-gray)

## 预备知识

### eureka元数据

Eureka的元数据有两种，分别为标准元数据和自定义元数据。

> **标准元数据：**主机名、IP地址、端口号、状态页和健康检查等信息，这些信息都会被发布在服务注册表中，用于服务之间的调用。
>
> **自定义元数据：**自定义元数据可以使用`eureka.instance.metadata-map`配置，这些元数据可以在远程客户端中访问，但是一般不会改变客户端的行为，除非客户端知道该元数据的含义



### eureka RestFul接口

| 请求名称                   | 请求方式 | HTTP地址                                                 | 请求描述                                                     |
| -------------------------- | -------- | -------------------------------------------------------- | ------------------------------------------------------------ |
| 注册新服务                 | POST     | /eureka/apps/`{appID}`                                   | 传递JSON或者XML格式参数内容，HTTP code为204时表示成功        |
| 取消注册服务               | DELETE   | /eureka/apps/`{appID}`/`{instanceID}`                    | HTTP code为200时表示成功                                     |
| 发送服务心跳               | PUT      | /eureka/apps/`{appID}`/`{instanceID}`                    | HTTP code为200时表示成功                                     |
| 查询所有服务               | GET      | /eureka/apps                                             | HTTP code为200时表示成功，返回XML/JSON数据内容               |
| 查询指定appID的服务列表    | GET      | /eureka/apps/`{appID}`                                   | HTTP code为200时表示成功，返回XML/JSON数据内容               |
| 查询指定appID&instanceID   | GET      | /eureka/apps/`{appID}`/`{instanceID}`                    | 获取指定appID以及InstanceId的服务信息，HTTP code为200时表示成功，返回XML/JSON数据内容 |
| 查询指定instanceID服务列表 | GET      | /eureka/apps/instances/`{instanceID}`                    | 获取指定instanceID的服务列表，HTTP code为200时表示成功，返回XML/JSON数据内容 |
| 变更服务状态               | PUT      | /eureka/apps/`{appID}`/`{instanceID}`/status?value=DOWN  | 服务上线、服务下线等状态变动，HTTP code为200时表示成功       |
| 变更元数据                 | PUT      | /eureka/apps/`{appID}`/`{instanceID}`/metadata?key=value | HTTP code为200时表示成功                                     |



### 更改自定义元数据

配置文件方式：

```java
eureka.instance.metadata-map.version = v1
```

接口请求：

```java
PUT                  /eureka/apps/{appID}/{instanceID}/metadata?key=value
```



## 前言

​    在平时的业务开发过程中，后端服务与服务之间的调用往往通过`fegin`或者`resttemplate`两种方式。但是我们在调用服务的时候往往只需要写服务名就可以做到路由到具体的服务，这其中的原理相比大家都知道是`SpringCloud`的`ribbon`组件帮我们做了负载均衡的功能。 

   灰度的核心就是路由，如果我们能够重写ribbon默认的负载均衡算法是不是就意味着我们能够控制服务的转发呢？是的！



## 调用链分析

### 外部调用

 - 请求==>zuul==>服务

> zuul在转发请求的时候,也会根据`Ribbon`从服务实例列表中选择一个对应的服务,然后选择转发.

### 内部调用

 - 请求==>zuul==>服务Resttemplate调用==>服务 
 - 请求==>zuul==>服务Fegin调用==>服务

> 无论是通过`Resttemplate`还是`Fegin`的方式进行服务间的调用,他们都会从`Ribbon`选择一个服务实例返回.

上面几种调用方式应该涵盖了我们平时调用中的场景,无论是通过哪种方式调用(排除直接ip:port调用),最后都会通过`Ribbon`,然后返回服务实例.





## 设计思路

首先，我们通过更改服务在eureka的元数据标识该服务为灰度服务，笔者这边用的元数据字段为`version`。

　

1.首先更改服务元数据信息，标记其灰度版本。通过eureka RestFul接口或者配置文件添加如下信息`eureka.instance.metadata-map.version=v1`



2.自定义`zuul`拦截器`GrayFilter`。此处笔者获取的请求头为token，然后将根据JWT的思想获取userId，然后获取灰度用户列表及其灰度版本信息，判断该用户是否为灰度用户。

若为灰度用户，则将灰度版本信息`version`存放在线程变量里面。**此处的线程不能用`Threadlocal`，因为SpringCloud用hystrix做线程池隔离，而线程池是无法获取到ThreadLocal中的信息的!**  所以这个时候我们可以参考`Sleuth`做分布式链路追踪的思路或者使用阿里开源的`TransmittableThreadLocal`方案。此处使用`HystrixRequestVariableDefault`实现跨线程池传递线程变量。



3.zuul拦截器处理完毕后，会经过ribbon组件从服务实例列表中获取一个实例选择转发。Ribbon默认的`Rule为`ZoneAvoidanceRule`。而此处我们继承该类，重写了其父类选择服务实例的方法。

以下为Ribbon源码：

```java
public abstract class PredicateBasedRule extends ClientConfigEnabledRoundRobinRule {
   // 略....
    @Override
    public Server choose(Object key) {
        ILoadBalancer lb = getLoadBalancer();
        Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(lb.getAllServers(), key);
        if (server.isPresent()) {
            return server.get();
        } else {
            return null;
        }       
    }
}
```

以下为自定义实现的伪代码:

```java
public class GrayMetadataRule extends ZoneAvoidanceRule {
   // 略....
    @Override
    public Server choose(Object key) {
	  //1.从线程变量获取version信息
        String version = HystrixRequestVariableDefault.get();
        
      //2.获取服务实例列表
        List<Server> serverList = this.getPredicate().getEligibleServers(this.getLoadBalancer().getAllServers(), key);
        
       //3.循环serverList，选择version匹配的服务并返回
        		for (Server server : serverList) {
			Map<String, String> metadata = ((DiscoveryEnabledServer) server).getInstanceInfo().getMetadata();

			String metaVersion = metadata.get("version);
			if (!StringUtils.isEmpty(metaVersion)) {
				if (metaVersion.equals(hystrixVer)) {
					return server;
				}
			}
		}
    }
}
```



4.此时，只是已经完成了   **请求==》zuul==》zuul拦截器==》自定义ribbon负载均衡算法==》灰度服务**这个流程，并没有涉及到 **服务==》服务**的调用。 

服务到服务的调用无论是通过resttemplate还是fegin，最后也会走ribbon的负载均衡算法，即**服务==》Ribbon 自定义Rule==》服务**。因为此时自定义的`GrayMetadataRule`并不能从线程变量中取到version，因为已经到了另外一个服务里面了。



5.此时依然可以参考`Sleuth`的源码`org.springframework.cloud.sleuth.Span`，这里不做赘述只是大致讲一下该类的实现思想。 就是在请求里面添加请求头，以便下个服务能够从请求头中获取信息。

> 此处，我们可以通过在**步骤2**中，让zuul添加添加线程变量的时候也在请求头中添加信息。然后，再自定义`HandlerInterceptorAdapter`拦截器，使之在到达服务之前将请求头中的信息存入到线程变量HystrixRequestVariableDefault中。
>
> 然后服务再调用另外一个服务之前，设置resttemplate和fegin的拦截器，添加头信息。

**resttemplate拦截器**

```java
public class CoreHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(request);
        String hystrixVer = CoreHeaderInterceptor.version.get();
        requestWrapper.getHeaders().add(CoreHeaderInterceptor.HEADER_VERSION, hystrixVer);
        return execution.execute(requestWrapper, body);
    }
}
```

**fegin拦截器**

```java
public class CoreFeignRequestInterceptor implements RequestInterceptor {
   @Override
   public void apply(RequestTemplate template) {
        String hystrixVer = CoreHeaderInterceptor.version.get();
        logger.debug("====>fegin version:{} ",hystrixVer); 
      template.header(CoreHeaderInterceptor.HEADER_VERSION, hystrixVer);
   }

}
```



6.到这里基本上整个请求流程就比较完整了,但是我们怎么让Ribbon使用***自定义的Rule***([传送门][3])?这里其实非常简单,只需要在服务的配置文件中配置一下代码即可.
``` 
yourServiceId.ribbon.NFLoadBalancerRuleClassName=自定义的负载均衡策略类
```
但是这样配置需要指定服务名,意味着需要在每个服务的配置文件中这么配置一次,所以需要对此做一下扩展.打开源码`org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration`类,该类是Ribbon的默认配置类.可以清楚的发现该类注入了一个`PropertiesFactory`类型的属性,可以看到`PropertiesFactory`类的构造方法

```
	public PropertiesFactory() {
		classToProperty.put(ILoadBalancer.class, "NFLoadBalancerClassName");
		classToProperty.put(IPing.class, "NFLoadBalancerPingClassName");
		classToProperty.put(IRule.class, "NFLoadBalancerRuleClassName");
		classToProperty.put(ServerList.class, "NIWSServerListClassName");
		classToProperty.put(ServerListFilter.class, "NIWSServerListFilterClassName");
	}
```
所以,我们可以继承该类从而实现我们的扩展,这样一来就不用配置具体的服务名了.至于Ribbon是如何工作的,这里有一篇方志明的文章([传送门][4])可以加强对Ribbon工作机制的理解



7.到这里基本上整个请求流程就比较完整了,上述例子中是以用户ID作为灰度的维度,当然这里可以实现更多的灰度策略,比如IP等,基本上都可以基于此方式做扩展



## 实现流程

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



## 灰度使用

配置文件示例

```java
spring.application.name = provide-test
server.port = 7770
eureka.client.service-url.defaultZone = http://localhost:1111/eureka/

#启动后直接将该元数据信息注册到eureka
#eureka.instance.metadata-map.version = v1
```

### 测试案例

​    分别启动四个测试实例，有version代表灰度服务，无version则为普通服务。当灰度服务测试没问题的时候，通过PUT请求eureka接口将version信息去除，使其变成普通服务.

**实例列表**：

- [x] zuul-server

- [x] provider-test
  `port:7770  version:无`
  `port: 7771  version:v1`

- [x] consumer-test

  `port:8880  version:无`

  `port: 8881  version:v1`

  

**修改服务信息**

​     服务在eureka的元数据信息可通过接口http://localhost:1111/eureka/apps访问到。

**服务信息实例：**

访问接口查看信息http://localhost:1111/eureka/apps/PROVIDE-TEST

![服务info信息](https://image-static.segmentfault.com/330/610/3306102598-5c1895b78c89b)

**注意事项**

> ​    通过此种方法更改server的元数据后，由于ribbon会缓存实力列表，所以在测试改变服务信息时，ribbon并不会立马从eureka拉去最新信息m，这个拉取信息的时间可自行配置。
>
> 同时，当服务重启时服务会重新将配置文件的version信息注册上去。



### 测试演示

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

![](https://image-static.segmentfault.com/405/525/405525019-5c18a560541ab)



> 

