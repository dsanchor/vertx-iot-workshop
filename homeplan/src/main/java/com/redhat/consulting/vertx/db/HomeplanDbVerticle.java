package com.redhat.consulting.vertx.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.redhat.consulting.vertx.MainVerticle;
import com.redhat.consulting.vertx.model.Homeplan;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

/**
 * Verticle that will expose DB accesses to the service
 * 
 * @author dsancho
 *
 */
public class HomeplanDbVerticle extends AbstractVerticle {

	public static final String HOMEPLAN_DB_SERVICE_ADDRESS = "homeplan.db";

	public static final String OPERATION_HEADER = "db-operation";
	public static final String HOMEPLAN_ID_HEADER = "homeplan-id";

	private static final String HOMEPLAN_COLLECTION_NAME = "homeplans";

	public enum Operation {
		GETALL, GETONE, CREATE, UPDATE, DELETE
	};

	private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	private MongoClient mongoClient;

	@Override
	public void start(Future<Void> startFuture) {

		// create pool for Mongo access
		// TODO refactor and get info from config map
		DeploymentOptions options = buildDeploymentOptions();

		mongoClient = MongoClient.createShared(vertx, options.getConfig());
		logger.info("Mongo client initialized");

		// mongoClient = MongoClient.createShared(vertx, config());

		// consume messages
		MessageConsumer<String> consumer = vertx.eventBus().consumer(HOMEPLAN_DB_SERVICE_ADDRESS);
		consumer.handler(message -> {
			logger.info("Message received in Mongo client!");
			logger.info("Content: \n" + message.body());
			String operation = message.headers().get(OPERATION_HEADER);
			if (operation != null && !"".equals(operation)) {
				try {
					Operation op = Operation.valueOf(operation.toUpperCase());
					switch (op) {
					case GETALL:
						Future<List<Homeplan>> futureHomePlans = Future.future();
						getAll(futureHomePlans);
						futureHomePlans.compose(response -> {
							message.reply(Json.encodePrettily(futureHomePlans.result()));
						}, Future.future().setHandler(handler -> {
							message.fail(500, "Error getting homeplans");
						}));
						break;
					case GETONE:
						Future<Homeplan> futureHomePlan = Future.future();
						getOne(message.headers().get(HOMEPLAN_ID_HEADER), futureHomePlan);
						futureHomePlan.compose(response -> {
							if (futureHomePlan.result() != null) {
								message.reply(Json.encodePrettily(futureHomePlan.result()));
							} else {
								message.fail(404, "Homeplan does not exist");
							}
						}, Future.future().setHandler(handler -> {
							message.fail(500, "Error getting homeplan");
						}));

						break;
					case CREATE:
						Future<Homeplan> futureHomePlanCreate = Future.future();
						save(Json.decodeValue(message.body(), Homeplan.class), futureHomePlanCreate);
						futureHomePlanCreate.compose(response -> {
							message.reply(Json.encodePrettily(futureHomePlanCreate.result()));
						}, Future.future().setHandler(handler -> {
							message.fail(500, "Error creating homeplan");
						}));
						break;
					case UPDATE:
						Future<Homeplan> futureHomePlanUpdate = Future.future();
						update(message.headers().get(HOMEPLAN_ID_HEADER),
								Json.decodeValue(message.body(), Homeplan.class), futureHomePlanUpdate);
						futureHomePlanUpdate.compose(response -> {
							message.reply(Json.encodePrettily(futureHomePlanUpdate.result()));
						}, Future.future().setHandler(handler -> {
							if (futureHomePlanUpdate.cause() instanceof ReplyException) {
								message.fail(((ReplyException) futureHomePlanUpdate.cause()).failureCode(),
										((ReplyException) futureHomePlanUpdate.cause()).getMessage());
							} else {
								message.fail(500, "Error updating homeplan");
							}
						}));
						break;
					case DELETE:
						Future<Void> futureHomePlanDelete = Future.future();
						delete(message.headers().get(HOMEPLAN_ID_HEADER), futureHomePlanDelete);
						futureHomePlanDelete.compose(response -> {
							message.reply(futureHomePlanDelete.result());
						}, Future.future().setHandler(handler -> {
							message.fail(500, "Error deleting homeplan");
						}));
						break;
					}

				} catch (IllegalArgumentException e) {
					message.fail(404, "Wrong operation in header");
				}

			} else {
				message.fail(404, "No operation in header");
			}
		});

	}

	private void getAll(Future<List<Homeplan>> future) {
		mongoClient.find(HOMEPLAN_COLLECTION_NAME, new JsonObject(), res -> {
			if (res.succeeded()) {
				List<JsonObject> list = res.result();
				if (list != null && !list.isEmpty()) {
					List<Homeplan> homeplans = list.stream().map(json -> new Homeplan(json))
							.collect(Collectors.toList());
					future.complete(homeplans);
				} else {
					future.complete(new ArrayList<Homeplan>());
				}
			} else {
				future.fail(res.cause());
			}
		});
	}

	private void getOne(String id, Future<Homeplan> future) {
		mongoClient.find(HOMEPLAN_COLLECTION_NAME, new JsonObject().put("id", id), res -> {
			if (res.succeeded()) {
				List<JsonObject> list = res.result();
				if (list != null && !list.isEmpty()) {
					Optional<JsonObject> result = res.result().stream().findFirst();
					if (result.isPresent()) {
						future.complete(new Homeplan(result.get()));
					} else {
						future.complete(null);
					}
				} else {
					future.complete(null);
				}
			} else {
				future.fail(res.cause());
			}
		});
	}

	private void update(String id, Homeplan homeplan, Future<Homeplan> future) {
		Future<Homeplan> futureGet = Future.future();
		getOne(id, futureGet);
		futureGet.compose(s -> {
			if (futureGet.result() != null) {
				save(homeplan, future);
			} else {
				future.fail(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, 404, "Homeplan does not exist"));
			}
		}, Future.future().setHandler(handler -> {
			logger.error("Homeplan consumer error, replying failure", handler.cause());
			future.fail("Homeplan update error");
		}));
	}

	private void save(Homeplan homeplan, Future<Homeplan> future) {
		mongoClient.save(HOMEPLAN_COLLECTION_NAME, toDocument(homeplan), res -> {
			if (res.succeeded()) {
				logger.info("Saved homeplan with id " + homeplan.getId());
				future.complete(homeplan);
			} else {
				future.fail(res.cause());
			}
		});
	}

	private void delete(String id, Future<Void> future) {
		mongoClient.removeDocument(HOMEPLAN_COLLECTION_NAME, new JsonObject().put("id", id), res -> {
			if (res.succeeded()) {
				future.complete();
			} else {
				future.fail(res.cause());
			}
		});
	}

	/**
	 * Add _id to Json sent to mongo
	 * 
	 * @param homeplan
	 * @return
	 */
	private JsonObject toDocument(Homeplan homeplan) {
		JsonObject document = homeplan.toJson();
		document.put("_id", homeplan.getId());
		return document;
	}

	private DeploymentOptions buildDeploymentOptions() {
		// DeploymentOptions options = new DeploymentOptions()
		// .setConfig(new JsonObject().put("db_name",
		// "homeplandb").put("username", "mongo")
		// .put("password", "mongo").put("connection_string",
		// "mongodb://homeplan-mongodb:27017"));
		DeploymentOptions options = new DeploymentOptions()
				.setConfig(new JsonObject().put("db_name", System.getenv("HOMEPLAN_DB_NAME"))
						.put("username", System.getenv("HOMEPLAN_DB_USERNAME"))
						.put("password", System.getenv("HOMEPLAN_DB_PASSWORD"))
						.put("connection_string", System.getenv("HOMEPLAN_DB_CONNECTION_STRING")));
		return options;
	}

}
