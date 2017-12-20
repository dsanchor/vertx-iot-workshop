package com.redhat.consulting.vertx;

import com.redhat.consulting.vertx.manager.DeviceManagerVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		vertx.deployVerticle(new DeviceManagerVerticle(), res -> {
			if (res.succeeded()) {
				startFuture.complete();
				logger.info("Service deployed!");
			} else {
				startFuture.fail(res.cause());
				logger.error("Deployment failed!", res.cause());
			}
		});

	}

}
