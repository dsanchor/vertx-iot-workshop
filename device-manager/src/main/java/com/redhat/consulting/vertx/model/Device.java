package com.redhat.consulting.vertx.model;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

public class Device implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String type;
	
	private String id;
	
	public Device() {
		super();
	}
	
	public Device(JsonObject jsonObject) {
		super();
		setId(jsonObject.getString("id"));
		setType(jsonObject.getString("type"));
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Device [type=" + type + ", id=" + id + "]";
	}
	
	public JsonObject toJson() {
		final JsonObject device = new JsonObject();
		device.put("id", getId());
		device.put("type", getType());
		return device;
	}

}
