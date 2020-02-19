package com.limitofsoul.offline

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

/**
 *
 * 基于隐语义模型的协同过滤
 */

//只需要rating数据，为了区别mllib里自带的Rating类
case class MovieRating(uid: Int, mid: Int, score: Double, timestamp: Int)

//定义基于预测评分的用户推荐列表
case class UserRecs(uid: Int, recs: Seq[Recommendation])

//定义基于LFM电影特征向量的电影相似度列表
case class MovieRecs(mid: Int, recs: Seq[Recommendation])

object LFMRecommender {

  //定义表名和常量
  val MONGODB_RATING_COLLECTION = "Rating"
  val USER_RECS = "UserRecs"
  val MOVIE_RECS = "MovieRecs"
  val USER_MAX_RECOMMENDATION = 20

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://root:xiaokaixian@101.133.167.244:27017/recommender?authSource=admin&readPreference=primary&appname=MongoDB%20Compass%20Community&ssl=false",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("LFMRecommender")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加载数据
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .rdd
      .map(rating => (rating.uid, rating.mid, rating.score))
      .cache()

    //从rating数据中提取所有的uid和mid，并且去重
    val userRDD = ratingRDD.map(_._1).distinct()
    val movieRDD = ratingRDD.map(_._2).distinct()

    //训练隐语义模型
    val trainData = ratingRDD.map(x => org.apache.spark.mllib.recommendation.Rating(x._1, x._2, x._3))

    val (rank, iterations, lambda) = (50, 5, 0.01)
    val model = ALS.train(trainData, rank, iterations, lambda)

    //基于用户和电影的隐特征，计算预测评分，得到用户推荐列表
    //计算user和movie的笛卡尔积，得到一个空评分矩阵
    val userMovies = userRDD.cartesian(movieRDD)

    //调用model的predict()预测评分
    val predRatings = model.predict(userMovies)

    val userRecs = predRatings
      .filter(_.rating > 0) //过滤出评分大于0的项
      .map(rating => (rating.user, (rating.product, rating.rating)))
      .groupByKey()
      .map {
        case (uid, recs) => UserRecs(uid, recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1, x._2)))
      }
      .toDF()

    userRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", USER_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //副产品：基于电影隐特征，计算相似度，得到电影的相似度列表，为实时推荐做基础
    val movieFeatures = model.productFeatures.map {
      case (mid, features) => (mid, new DoubleMatrix(features))
    }

    //对所有电影两两计算它们对相似度，先做笛卡尔积
    val movieRecs = movieFeatures.cartesian(movieFeatures)
      .filter {
        //把自己和自己对配对过滤掉
        case (a, b) => a._1 != b._1
      }
      .map {
        case (a, b) => {
          val simScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, simScore))
        }
      }
      .filter(_._2._2 > 0.6) //过滤出相似度大于0.6的
      .groupByKey()
      .map {
        case (mid, items) => MovieRecs(mid, items.toList.sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)))
      }
      .toDF()

    movieRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", MOVIE_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    spark.stop()
  }

  //求向量余弦相似度
  def consinSim(movie1: DoubleMatrix, movie2: DoubleMatrix): Double = {
    movie1.dot(movie2) / (movie1.norm2() * movie2.norm2())
  }

}
