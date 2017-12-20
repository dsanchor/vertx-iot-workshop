package com.redhat.consulting.vertx.manager;

import com.redhat.consulting.vertx.model.Room;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeviceManagerVerticle extends AbstractVerticle {

	public static final String DEVICE_REGISTRATION_SERVICE_ADDRESS = "devices";

	public static final String DEVICE_OPERATION_HEADER = "device-operation";

	public enum DeviceManagerOperation {
		REGISTER, UNREGISTER
	};

	private final Logger logger = LoggerFactory.getLogger(DeviceManagerVerticle.class);

	@Override
	public void start() throws Exception {

		MessageConsumer<String> consumer = vertx.eventBus().consumer(DEVICE_REGISTRATION_SERVICE_ADDRESS);
		consumer.handler(message -> {
			logger.info("Message received: " +message.body());
			String operation = message.headers().get(DEVICE_OPERATION_HEADER);
			if (operation != null && !"".equals(operation)) {
				DeviceManagerOperation op = DeviceManagerOperation.valueOf(operation.toUpperCase());
				switch (op) {
				case REGISTER:
					register(Json.decodeValue(message.body(), Room.class));
					break;
				case UNREGISTER:
					unregister(Json.decodeValue(message.body(), Room.class));
					break;
				}
			}
		});
	}

	private void register(Room room) {
		logger.info("Registering devices");

		
	}
	
	private void unregister(Room decodeValue) {
		logger.info("Unregistering devices");
		
	}
}
