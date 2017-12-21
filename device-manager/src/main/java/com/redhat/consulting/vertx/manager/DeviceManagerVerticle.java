package com.redhat.consulting.vertx.manager;

import java.util.HashMap;
import java.util.Map;

import com.redhat.consulting.vertx.model.Device;
import com.redhat.consulting.vertx.model.Room;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeviceManagerVerticle extends AbstractVerticle {

	public static final String DEVICE_REGISTRATION_SERVICE_ADDRESS = "devices";

	public static final String DEVICE_OPERATION_HEADER = "device-operation";
	
	public static final String HOMEPLAN_ID_HEADER = "homeplan-id";

	public enum DeviceManagerOperation {
		REGISTER, UNREGISTER
	};

	private final Logger logger = LoggerFactory.getLogger(DeviceManagerVerticle.class);

	@Override
	public void start() throws Exception {

		MessageConsumer<String> consumer = vertx.eventBus().consumer(DEVICE_REGISTRATION_SERVICE_ADDRESS);
		consumer.handler(message -> {
			logger.info("Message received: " + message.body());
			String operation = message.headers().get(DEVICE_OPERATION_HEADER);
			if (operation != null && !"".equals(operation)) {
				DeviceManagerOperation op = DeviceManagerOperation.valueOf(operation.toUpperCase());
				String homeplanId = message.headers().get(HOMEPLAN_ID_HEADER);
				switch (op) {
				case REGISTER:
					register(Json.decodeValue(message.body(), Room.class), homeplanId);
					break;
				case UNREGISTER:
					unregister(Json.decodeValue(message.body(), Room.class), homeplanId);
					break;
				}
			}
		});
	}

	// FIXME use sync workers
	private void register(Room room, String homeplanId) {
		logger.info("Registering devices");
		OpenShiftClient client = new DefaultOpenShiftClient();
		for (Device device : room.getDevices()) {
			String dcName = createName(homeplanId, room.getId(), device.getId());
			// TODO fix image reference.. I just put one for testing (it should be obtained from param/envvar
			logger.info("Creating device: " + dcName);
			// create labels
			Map<String, String> labels = new HashMap<>();
			labels.put("homeplan", homeplanId);
			labels.put("room", room.getId());
			labels.put("device", device.getId());
			// create DC
			client.deploymentConfigs().createOrReplaceWithNew().withNewMetadata().withName(dcName)
					.withLabels(labels).endMetadata().withNewSpec().withReplicas(1).addNewTrigger()
					.withType("ConfigChange").endTrigger().addToSelector("device", device.getId()).withNewTemplate()
					.withNewMetadata().addToLabels("device", device.getId()).endMetadata().withNewSpec()
					.addNewContainer().withName("device")
					.withImage("docker-registry.default.svc:5000/"+client.getNamespace()+"/sensor:latest").endContainer()
					.endSpec().endTemplate().endSpec().done();
			logger.info("Device created");
		}
	}

	// FIXME use sync workers
	private void unregister(Room decodeValue, String homeplanId) {
		logger.info("Unregistering devices");
		OpenShiftClient client = new DefaultOpenShiftClient();
		for (Device device : room.getDevices()) {
			String dcName = createName(homeplanId, room.getId(), device.getId());
			// TODO fix image reference.. I just put one for testing (it should be obtained from param/envvar
			logger.info("Removing device: " + dcName);
			client.deploymentConfigs().withName(dcName).delete();
			logger.info("Device removed");
		}

	}
	
	private String createName(String homeplanId, String room, String device) {
		return homeplanId +"-"+room+"-"+device;
	}
}
