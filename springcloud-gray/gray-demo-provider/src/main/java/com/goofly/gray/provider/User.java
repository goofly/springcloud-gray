package com.goofly.gray.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * @author goofly
 * @E-mail 709233178@qq.com
 * @date 2019/1/21
 */
@Data
@AllArgsConstructor
@ToString
public class User {
    private Long id;
    private String account;
    private String ip;
    private String port;
	public User(Long id, String ip,String account) {
		super();
		this.id = id;
		this.ip = ip;
		this.account = account;
	}
}
