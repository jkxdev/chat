package com.cmad.demo.chat_websocket;

public class AuthData {
	private String name;
	private String tok;
	public AuthData() {

	}
	public AuthData(String uname) {
		name = uname;
		System.out.println("AuthData.AuthData() with uname = " +uname);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTok() {
		return tok;
	}
	public void setTok(String toki) {
		this.tok = toki;
		System.out.println("AuthData.setTok() token is : "+tok);
	}

	public String toString() {
		return "name:" + name + "tok:" + tok;
	}
}
