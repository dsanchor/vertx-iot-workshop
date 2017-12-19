package com.redhat.consulting.vertx;

import com.redhat.consulting.vertx.db.HomeplanDbVerticle;
import com.redhat.consulting.vertx.rest.RestVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

	private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		Future<String> dbVerticleDeployment = Future.future();
		vertx.deployVerticle(new HomeplanDbVerticle(), dbVerticleDeployment.completer());

		dbVerticleDeployment.compose(id -> {

			Future<String> httpVerticleDeployment = Future.future();
			vertx.deployVerticle(new RestVerticle(), httpVerticleDeployment.completer());
			return httpVerticleDeployment;

		}).setHandler(ar -> {
			if (ar.succeeded()) {
				startFuture.complete();
				logger.info("Service deployed!");
			} else {
				startFuture.fail(ar.cause());
				logger.error("Deployment failed!", ar.cause());
			}
		});
	}

}
