package com.goofly.gray.zuul.apollo;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "zuul.gray")
public class GrayUserConfigProp {
	
	@Builder.Default
	private List<String> userIdList = Collections.emptyList();
	private String version;
}