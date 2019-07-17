# Recommender Engine - Streaming Component

## Description
This repository contains the streaming component of the recommender engine. The component is written using Apache Spark.

The code in this repository has a simple structure. It does the following things:
* Fetches streaming user reviews from Kafka.
* Loads a learning model from a path specified in the arguments. (For the learning model, we use ALSModel.)
* Generates recommendations for users by using the learning model.
* Stores the generated recommendations in MongoDB.

## Usage
### Run
This component cannot be run by itself. In order to run this component, all of the project components must be run using `docker-compose`. See [Recommender Engine - Docker Files](https://github.com/trendyol-data-eng-summer-intern-2019/recom-engine-docker)

If you want to change the code and run, after you change the code do the following steps:

* Go to the root directory of the repository.
* Run `sbt clean assembly`
* Get the recom-engine-docker repository from [here](https://github.com/trendyol-data-eng-summer-intern-2019/recom-engine-docker).
* Move the jar file that is created from `target/scala-2.11/recom-engine-streaming-assembly-0.1.jar` to `images/spark/master/target` under recom-engine-docker's root directory.
* Go to the root diretory of recom-engine-docker and run the command `docker-compose up`.

### Watch the Outputs
If you want to watch the outputs of the streaming, do the following steps:

* First run all of the components with `docker-compose`.
* Go to the worker's web page from [http://localhost:8082](http://localhost:8082).
* In this page, you can see the running drivers, click [stdout]() link of the streaming application, then you can watch the outputs of the application.

## Members
- [Oğuzhan Bölükbaş](https://github.com/oguzhan-bolukbas)
- [Sercan Ersoy](https://github.com/sercanersoy)
- [Yasin Uygun](https://github.com/yasinuygun)
