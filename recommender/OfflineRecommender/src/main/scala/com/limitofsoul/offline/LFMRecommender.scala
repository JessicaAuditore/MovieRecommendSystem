package com.limitofsoul.offline

/**
 *
 * 基于隐语义模型的协同过滤
 */

//只需要rating数据，为了区别mllib里自带的Rating类
case class MovieRating(uid: Int, mid: Int, score: Double, timestamp: Int)

case class MongoConfig(uri: String, db: String)

//定义一个基准推荐对象
case class Recommendation(mid: Int, score: Double)

//定义基于预测评分的用户推荐列表
case class UserRecs(uid: Int, rec: Seq[Recommendation])


object LFMRecommender {

}
