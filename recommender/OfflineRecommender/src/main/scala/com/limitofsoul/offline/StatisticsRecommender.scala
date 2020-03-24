package com.limitofsoul.offline

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
 *
 * TODO:离线统计服务
 */

//定义电影类别top10推荐对象
case class GenresRecommendation(genres: String, recs: Seq[Recommendation])

object StatisticsRecommender {

  //定义表名
  val RATE_MORE_MOVIES = "RateMoreMovies"
  val RATE_MORE_RECENTLY_MOVIES = "RateMoreRecentlyMovies"
  val AVERAGE_MOVIES = "AverageMovies"
  val GENRES_TOP_MOVIES = "GenresTopMovies"

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf().setMaster(Common.config("spark.cores")).setAppName("StatisticsRecommender")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._
    val mongoConfig = MongoConfig(Common.config("mongo.uri"), Common.config("mongo.db"))

    //从mongodb加载数据
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", Common.MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .toDF()

    val movieDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", Common.MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .toDF()

    //创建名为ratings的临时表
    ratingDF.createTempView("ratings")

    /**
     *
     * TODO:1.历史热门统计：历史评分数据最多 mid,count
     */
    val rateMoreMoviesDF = spark.sql("select mid,count(mid) as count from ratings group by mid")

    Common.storeDFInMongoDB(rateMoreMoviesDF, RATE_MORE_MOVIES)


    /**
     *
     * TODO:2.近期热门统计：按照"yyyyMM"格式选取最近的评分数据，统计评分个数
     */
    //创建一个日期格式化工具
    val simpleDateFormat = new SimpleDateFormat("yyyyMM")
    //注册udf，把时间戳转换成年月格式
    spark.udf.register("changeDate", (x: Int) => simpleDateFormat.format(new Date(x * 1000L)).toInt)

    //对原始数据做预处理，去掉uid
    val ratingOfYearMonth = spark.sql("select mid,score,changeDate(timestamp) as yearmonth from ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")

    //从ratingOfMonth中查找电影在各个月份的评分 mid,count,yearmonth
    val rateMoreRecentlyMoviesDF = spark.sql("select mid,count(mid) as count,yearmonth from ratingOfMonth group by yearmonth,mid order by yearmonth desc,count desc")

    Common.storeDFInMongoDB(rateMoreRecentlyMoviesDF, RATE_MORE_RECENTLY_MOVIES)


    /**
     *
     * TODO:3.优质电影统计：统计电影的平均评分 mid,avg
     */
    val averageMoviesDF = spark.sql("select mid,avg(score) as avg from ratings group by mid")

    Common.storeDFInMongoDB(averageMoviesDF, AVERAGE_MOVIES)


    /**
     *
     * TODO:4.各类别电影Top统计
     */
    //定义所有类别
    val genres = List("Action", "Adventure", "Animation", "Children", "Comedy", "Crime", "Documentary", "Drama", "File-Noir", "Fantasy", "Horror", "IMAX", "Musical", "Mystery", "Romance", "Sci-Fi", "Thriller", "War", "Western")

    //把平均评分加入movie表中，加一列 inner join
    val movieWithScore = movieDF.join(averageMoviesDF, "mid")

    //做笛卡尔积，把genres转成rdd
    val genresRDD = spark.sparkContext.makeRDD(genres)

    //计算类别top10，首先对类别和电影做笛卡尔积
    val genresTopMoviesDF = genresRDD.cartesian(movieWithScore.rdd)
      .filter {
        //条件过滤，找出movie的字段genres值(Action|Adventure|Sci-Fi)包含当前类别genre(Action)的那些
        case (genre, movieRow) => movieRow.getAs[String]("genres").toLowerCase.contains(genre.toLowerCase)
      }
      .map {
        case (genre, movieRow) => (genre, (movieRow.getAs[Int]("mid"), movieRow.getAs[Double]("avg")))
      }
      .groupByKey()
      .map {
        case (genre, items) => GenresRecommendation(genre, items.toList
          .sortWith(_._2 > _._2).take(10)
          .map(item => Recommendation(item._1, item._2)))
      }
      .toDF()

    Common.storeDFInMongoDB(genresTopMoviesDF, GENRES_TOP_MOVIES)


    spark.stop()
  }
}
