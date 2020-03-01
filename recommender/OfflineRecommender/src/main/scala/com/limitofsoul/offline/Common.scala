package com.limitofsoul.offline

import org.apache.spark.sql.DataFrame

case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, language: String, genres: String, actors: String, directors: String)

//为了区别mllib里自带的Rating类(uid: Int, mid: Int, score: Double)
case class MovieRating(uid: Int, mid: Int, score: Double, timestamp: Int)

case class MongoConfig(uri: String, db: String)

case class Recommendation(mid: Int, score: Double)


object Common {

  //定义表名
  val MONGODB_MOVIE_COLLECTION = "Movie"
  val MONGODB_RATING_COLLECTION = "Rating"

  val config = Map(
    "spark.cores" -> "local[*]",
    "mongo.uri" -> "mongodb://localhost:27017/recommender",
    "mongo.db" -> "recommender"
  )

  def storeDFInMongoDB(df: DataFrame, collection_name: String): Unit = {
    val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }
}