package com.redhat.consulting.vertx.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

public class Homeplan implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String id;
	
	private List<Room> rooms;

	public Homeplan() {
		super();
	}

	@SuppressWarnings("unchecked")
	public Homeplan(JsonObject jsonObject) {
		super();
		setId(jsonObject.getString("id"));
		if (jsonObject.getJsonArray("rooms")!=null) {
			List<JsonObject> devices = jsonObject.getJsonArray("rooms").getList();
			setRooms(devices.stream().map(roomJsonObject -> new Room((JsonObject) roomJsonObject))
					.collect(Collectors.toList()));
		}
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Room> getRooms() {
		return rooms;
	}

	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}
	
	public JsonObject toJson() {
		final JsonObject homeplan = new JsonObject();
		homeplan.put("id", getId());
		if (getRooms()!=null) {
			homeplan.put("rooms", getRooms().stream().map(room -> room.toJson()).collect(Collectors.toList()));
		}
		return homeplan;
	}

}
