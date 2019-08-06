package com.trendyol.recomengine.streaming

import com.mongodb.spark.sql._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.recommendation.ALSModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Loads a machine learning model, gets user reviews in streaming manner from Kafka,
  * generates recommendations for users based on the learning model and writes these
  * recommendations to MongoDB.
  */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.length != 7) {
      println("Expected arguments: " +
        "<mongo-host:mongo-port> <mongo-database> <mongo-collection> " +
        "<kafka-host:kafka-port> <kafka-topic> " +
        "<spark-host:spark-port> <model-path>")
      System.exit(0)
    }

    val mongoHostPort = args(0)
    val mongoDatabase = args(1)
    val mongoCollection = args(2)
    val kafkaHostPort = args(3)
    val kafkaTopic = args(4)
    val sparkHostPort = args(5)
    val modelPath = args(6)

    val logger: Logger = Logger.getLogger("org")
    logger.setLevel(Level.ERROR)

    val spark: SparkSession = SparkSession.builder()
      .appName("recom-engine-streaming")
      .config("spark.mongodb.output.uri", "mongodb://" + mongoHostPort + "/" + mongoDatabase + "." + mongoCollection)
      .master("spark://" + sparkHostPort)
      .getOrCreate()

    import spark.implicits._

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafkaHostPort,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> (kafkaTopic + "Group"),
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val topics = Array(kafkaTopic)

    val ssc = new StreamingContext(spark.sparkContext, Seconds(5))

    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val model: ALSModel = ALSModel.load(modelPath)
    model.setColdStartStrategy("drop")

    stream.map(record => record.value).foreachRDD(rdd => {
      val reviews = spark.createDataFrame(rdd.map[Review](rdd => parseReview(rdd))).cache
      reviews.show()

      val recommendations = model.recommendForUserSubset(reviews, 10).as[RecommendationsWithPredictedScores].cache
      recommendations.show(false)

      val recommendation = recommendations.map(row => {
        Recommendations(row.userId, row.recommendations.map(row => row.productId.toString))
      })

      recommendation.withColumnRenamed("userId", "_id").saveToMongoDB
      reviews.unpersist
      recommendations.unpersist
    })

    ssc.start()
    ssc.awaitTermination()
  }

  /**
    * Parses the review that comes in this format: userId::productId::score::timestamp
    * @param reviewStr A review string in this format: userId::productId::score::timestamp
    * @return A Review object which is parsed from reviewStr.
    */
  def parseReview(reviewStr: String): Review = {
    val fields = reviewStr.split(",")
    assert(fields.size == 4)
    Review(fields(0), fields(1), fields(2).toFloat, fields(3).toLong)
  }
}

/**
  * @param productId A product's id that a user can give rating.
  * @param rating    A predicted rating that a user probably give to the product.
  */
case class Prediction(productId: Integer, rating: Float)

/**
  * @param userId          A user's id.
  * @param recommendations A list of products' id and their predicted ratings (@see Prediction)
  *                        that the user might give rating most.
  */
case class RecommendationsWithPredictedScores(userId: String, recommendations: Array[Prediction])

/**
  * @param userId          A user's id.
  * @param recommendations A list of products' id that the user might give rating most.
  */
case class Recommendations(userId: String, recommendations: Array[String])

/**
  * @param userId    The user's id who reviews a product.
  * @param productId The product's id which is reviewed by the user.
  * @param score     The score (rating) that the user give to the product.
  * @param timestamp The timestamp when the user reviews the product.
  */
case class Review(userId: String, productId: String, score: Float, timestamp: Long)
