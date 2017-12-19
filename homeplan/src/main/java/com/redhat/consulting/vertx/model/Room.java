package com.redhat.consulting.vertx.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

public class Room implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String type;

	private int temperature;

	private List<Device> devices;

	public Room() {
		super();
	}

	@SuppressWarnings("unchecked")
	public Room(JsonObject jsonObject) {
		super();
		setType(jsonObject.getString("type"));
		setTemperature(jsonObject.getInteger("temperature"));
		if (jsonObject.getJsonArray("devices")!=null) {
			List<JsonObject> devices = jsonObject.getJsonArray("devices").getList();
			setDevices(devices.stream().map(devJsonObject -> new Device((JsonObject) devJsonObject))
					.collect(Collectors.toList()));
		}
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	@Override
	public String toString() {
		return "Room [type=" + type + ", temperature=" + temperature + ", devices=" + devices + "]";
	}
	
	public JsonObject toJson() {
		final JsonObject room = new JsonObject();
		room.put("type", getType());
		room.put("temperature", getTemperature());
		if (getDevices()!=null) {
			room.put("devices", getDevices().stream().map(device -> device.toJson()).collect(Collectors.toList()));
		}
		return room;
	}

}
