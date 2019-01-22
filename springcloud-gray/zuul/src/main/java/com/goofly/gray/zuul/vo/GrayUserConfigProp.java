package com.goofly.gray.zuul.vo;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author goofly
 * @E-mail 709233178@qq.com
 * @date 2019/1/21
 */
@Component
public class GrayUserConfigProp {

    private List<String> userIdList = Arrays.asList("goofly","xli","bob");
    private String version = "v1";

    public List<String> getUserIdList() {
        return this.userIdList;
    }

    public String getVersion() {
        return this.version;
    }
}
