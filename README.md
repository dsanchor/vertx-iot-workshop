# vertx-iot-workshop

## Homeplan service

### Step by step configuration on openshift

`oc login -u user -p password https://ocp-domain.com`

Create project

`oc new-project vertx`

Clone application source code locally 

`git clone https://github.com/dsanchor/vertx-iot-workshop .`

Create, build and deploy application on Openshift (choose either binary or source to image builds)

1. Using binary builds:

`oc process -f homeplan/openshift/homeplan-binary-template.yml | oc create -f - -n vertx`

`cd homeplan`

`mvn clean package`

`oc start-build homeplan --from-file=target/homeplan-1.0-SNAPSHOT.jar`

2. Using git repository as source

`oc process -f homeplan/openshift/homeplan-source-template.yml | oc create -f - -n vertx`

### Test Rest API 

Download [Postman](https://www.getpostman.com/)

Import project located under homeplan/postman/vertx.postman_collection.json

Import environment configuration under homeplan/postman/online.postman_environment.json (modify properties if required)

Play with the examples request!


### Delete objects in openshift (homemplan service and mongodb)

Delete all (homemplan service and mongodb)

`oc delete all,secret -l microservice=homeplan`

Delete only homeplan rest service

`oc delete all -l app=homeplan`

Delete only mongodb

`oc delete all,secret -l app=homeplan-mongodb`
