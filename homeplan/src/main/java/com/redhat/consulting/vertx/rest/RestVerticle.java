package com.redhat.consulting.vertx.rest;

import com.redhat.consulting.vertx.MainVerticle;
import com.redhat.consulting.vertx.db.HomeplanDbVerticle;
import com.redhat.consulting.vertx.db.HomeplanDbVerticle.Operation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class RestVerticle extends AbstractVerticle {

	public static final String ROOT_PATH = "/homeplans";
	public static final String ID_PARAM = "id";

	private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	@Override
	public void start() {

		HttpServer server = vertx.createHttpServer();

		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create());

		router.get(ROOT_PATH).handler(this::getAll);
		router.post(ROOT_PATH).handler(this::create);
		router.get(ROOT_PATH + "/:" + ID_PARAM).handler(this::getOne);
		router.put(ROOT_PATH + "/:" + ID_PARAM).handler(this::update);
		router.delete(ROOT_PATH + "/:" + ID_PARAM).handler(this::delete);

		server.requestHandler(router::accept).listen(8080);
	}

	private void getAll(RoutingContext rc) {
		logger.info("Returning homeplans");
		vertx.eventBus().<String>send(HomeplanDbVerticle.HOMEPLAN_DB_SERVICE_ADDRESS, "",
				buildOptions(HomeplanDbVerticle.Operation.GETALL), reply -> {
					if (reply.succeeded()) {
						logger.info("Homeplans returned");
						rc.response().putHeader("content-type", "application/json; charset=utf-8")
								.end(reply.result().body());
					} else {
						manageReplyError(rc, reply);
					}
				});

	}

	private void getOne(RoutingContext rc) {
		logger.info("Returning homeplan");
		vertx.eventBus().<String>send(HomeplanDbVerticle.HOMEPLAN_DB_SERVICE_ADDRESS, "",
				buildOptionsForOne(HomeplanDbVerticle.Operation.GETONE, rc.request().getParam(ID_PARAM)), reply -> {
					if (reply.succeeded()) {
						if (reply.result()!=null) {
							logger.info("Homeplan returned");
							rc.response().putHeader("content-type", "application/json; charset=utf-8")
									.end(reply.result().body());
						} else {
							logger.info("Homeplan not found");
							rc.fail(404);
						}
					} else {
						manageReplyError(rc, reply);
					}
				});
	}

	private void create(RoutingContext rc) {
		logger.info("Creating homeplan: " + rc.getBodyAsString());
		vertx.eventBus().<String>send(HomeplanDbVerticle.HOMEPLAN_DB_SERVICE_ADDRESS, rc.getBodyAsString(),
				buildOptions(HomeplanDbVerticle.Operation.CREATE), reply -> {
					if (reply.succeeded()) {
						logger.info("Homeplan created");
						rc.response().putHeader("content-type", "application/json; charset=utf-8")
								.end(reply.result().body());
					} else {
						manageReplyError(rc, reply);
					}
				});
	}

	private void update(RoutingContext rc) {
		vertx.eventBus().<String>send(HomeplanDbVerticle.HOMEPLAN_DB_SERVICE_ADDRESS, rc.getBodyAsString(),
				buildOptionsForOne(HomeplanDbVerticle.Operation.UPDATE, rc.request().getParam(ID_PARAM)), reply -> {
					if (reply.succeeded()) {
						logger.info("Updating homeplan");
						rc.response().putHeader("content-type", "application/json; charset=utf-8")
								.end(reply.result().body());
					} else {
						manageReplyError(rc, reply);
					}
				});
	}

	private void delete(RoutingContext rc) {
		logger.info("Deleting homeplan");
		vertx.eventBus().<String>send(HomeplanDbVerticle.HOMEPLAN_DB_SERVICE_ADDRESS, "",
				buildOptionsForOne(HomeplanDbVerticle.Operation.DELETE, rc.request().getParam(ID_PARAM)), reply -> {
					if (reply.succeeded()) {
						logger.info("Homeplan deleted");
						rc.response().end();
					} else {
						manageReplyError(rc, reply);
					}
				});
	}

	private DeliveryOptions buildOptions(Operation operation) {
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader(HomeplanDbVerticle.OPERATION_HEADER, operation.toString());
		return options;
	}

	private DeliveryOptions buildOptionsForOne(Operation operation, String homeplanId) {
		DeliveryOptions options = buildOptions(operation);
		options.addHeader(HomeplanDbVerticle.HOMEPLAN_ID_HEADER, homeplanId);
		return options;
	}

	private void manageReplyError(RoutingContext rc, AsyncResult<Message<String>> asyncResult) {
		if (asyncResult.failed()) {
			ReplyException re = (ReplyException) asyncResult.cause();
			logger.error("Error in service caused by: " + re.getMessage());
			rc.fail(re.failureCode());
		}
	}
}
