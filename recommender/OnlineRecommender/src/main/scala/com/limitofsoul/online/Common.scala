package com.limitofsoul.online

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.kafka.common.serialization.StringDeserializer
import redis.clients.jedis.Jedis

case class MongoConfig(uri: String, db: String)

case class Recommendation(mid: Int, score: Double)

//定义连接助手对象，序列化
object ConnHelper extends Serializable {
  lazy val jedis = new Jedis(Common.config("redis.host"), Common.config("redis.port").toInt)
  lazy val mongoClient: MongoClient = MongoClient(MongoClientURI(Common.config("mongo.uri")))
}

object Common {

  val config = Map(
    "spark.cores" -> "local[*]",
    "mongo.uri" -> "mongodb://localhost:27017/recommender",
    "mongo.db" -> "recommender",
    "redis.host" -> "localhost",
    "redis.port" -> "6379",
    "kafka.topic" -> "recommender"
  )

  //定义kafka连接参数
  val kafkaParam = Map(
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "recommender",
    "auto.offset.reset" -> "latest"
  )
}