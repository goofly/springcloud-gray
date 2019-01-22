package com.goofly.gray.consumer.fegin;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PROVIDE-TEST")
public interface TestClient {

	@RequestMapping(value = "/user/getId", method = RequestMethod.GET)
	Object testGet(@RequestParam("id") Long id);

	@RequestMapping(value = "/user/post", method = RequestMethod.POST)
	Object testPost(@RequestBody String body);

}
