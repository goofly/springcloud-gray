package com.goofly.gray.zuul.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author goofly
 * @E-mail 709233178@qq.com
 * @date 2019/1/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "zuul.gray")
@Component
public class GrayUserConfigProp {

    @Builder.Default
    private List<String> userIdList = Collections.emptyList();
    private String version;
}
