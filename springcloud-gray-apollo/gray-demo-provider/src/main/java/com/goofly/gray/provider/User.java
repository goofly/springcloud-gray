package com.goofly.gray.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class User {
    private Long id;
    private String account;
    private String ip;
    private String port;
	public User(Long id, String account, String ip) {
		super();
		this.id = id;
		this.account = account;
		this.ip = ip;
	}
}
