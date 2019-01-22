package com.goofly.gray.provider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import com.netflix.appinfo.EurekaInstanceConfig;


@RestController
@RequestMapping("/user")
public class UserController {
	
    @Autowired
    Environment env;
    
    @Autowired
    EurekaInstanceConfig config;
    
    public static String getIp(){
    	String ip = null;
    	try {
    		ip=  InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	return ip;
    }
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private  Map<Long,User> users = new HashMap() {
		private static final long serialVersionUID = 1L;
		{
			put(111L, new User(111L, getIp(), "111-pwd"));
			put(222L, new User(222L,getIp(), "222-pwd"));
			put(333L, new User(333L,getIp(), "333-pwd"));
		}
	};
	
	
	@RequestMapping(value="/getId",method = RequestMethod.GET)
	public User getId(@RequestParam Long id) {
		 User user = users.get(id);
		 user.setPort(env.getProperty("server.port"));
		 return user;
		 
	}

	@RequestMapping(value="/post",method = RequestMethod.POST)
	public User insert(@RequestBody User user) {
		user.setPort(env.getProperty("server.port"));
		return user;
	}

}