package com.goofly.gray.consumer.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.goofly.gray.consumer.fegin.TestClient;
import com.google.common.collect.ImmutableMap;

/**
 * @author goofly
 * @E-mail 709233178@qq.com
 * @date 2019/1/21
 */
@RestController
@RequestMapping("/test")
public class TestController {

	@Autowired
	Environment env;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private TestClient testClient;

	private static final Logger logger = LoggerFactory.getLogger(TestController.class);

	@RequestMapping(value = "/me", method = RequestMethod.GET)
	public Object me(@RequestParam Long id) {
		logger.info("=======>id:{}", id);
		return "me:" + id;
	}

	// ==================================== resttemplate
	// =====================================================================

	@RequestMapping(value = "/getId", method = RequestMethod.GET)
	public Object getPath(@RequestHeader(value = "version", required = false) String version, @RequestParam Long id) {
		logger.info("=======>id:{},version:{}", id, version);

		Object result = restTemplate.getForObject("http://provide-test/user/getId?id=" + id, String.class);
		return ImmutableMap.of("port", env.getProperty("server.port"), "result", result);
	}

	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public Object postId(@RequestBody String param) {
		logger.info("=======>param:{}", param);

		Object result = restTemplate.postForObject("http://provide-test/user/post", param, String.class);
		return ImmutableMap.of("port", env.getProperty("server.port"), "result", result);
	}

	// =========================================== fegin
	// ==========================================================

	@RequestMapping(value = "/feginGet", method = RequestMethod.GET)
	public Object restTemplatePost(@RequestParam Long id) {
		Object result = testClient.testGet(id);
		return ImmutableMap.of("port", env.getProperty("server.port"), "result", result);
	}

	@RequestMapping(value = "/feginPost", method = RequestMethod.POST)
	public Object restTemplatePost(@RequestBody String body) {
		Object result = testClient.testPost(body);
		return ImmutableMap.of("port", env.getProperty("server.port"), "result", result);
	}

}
