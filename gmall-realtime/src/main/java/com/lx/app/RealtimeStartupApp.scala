package com.lx.app

import java.text.SimpleDateFormat
import java.util
import java.util.{Date, Properties}

import com.alibaba.fastjson.JSON
import com.lx.beans.StartUpLog
import com.lx.constants.GmallConstants
import com.lx.utils.{MyKafkaUtil, PropertiesUtil, RedisUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import redis.clients.jedis.Jedis

object RealtimeStartupApp {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("gmall")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(10))

    val startupStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstants.KAFKA_TOPIC_STARTUP, ssc)

    /*startupStream.map(_.value()).foreachRDD { rdd =>
      println(rdd.collect().mkString("\n"))
    }*/

    val startupLogDstream: DStream[StartUpLog] = startupStream.map(_.value()).map { log =>
      // println(s"log = ${log}")
      val startUpLog: StartUpLog = JSON.parseObject(log, classOf[StartUpLog])
      val date = new Date(startUpLog.ts)
      val datestr: String = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date)
      val dateArr: Array[String] = datestr.split(" ")
      startUpLog.logDate = dateArr(0)
      startUpLog.logHour = dateArr(1).split(":")(0)
      startUpLog.logHourMinute = dateArr(1)

      startUpLog
    }

    val filteredDstream: DStream[StartUpLog] = startupLogDstream.transform { rdd =>
      println("过滤前：" + rdd.count())
      //driver  //周期性执行
      val curdate: String = new SimpleDateFormat("yyyy-MM-dd").format(new Date)
      val jedis: Jedis = RedisUtil.getJedisClient
      val key = "dau:" + curdate
      val dauSet: util.Set[String] = jedis.smembers(key)
      val dauBC: Broadcast[util.Set[String]] = ssc.sparkContext.broadcast(dauSet)
      val filteredRDD: RDD[StartUpLog] = rdd.filter { startuplog =>
        //executor
        val dauSet: util.Set[String] = dauBC.value
        !dauSet.contains(startuplog.mid)
      }
      println("过滤后：" + filteredRDD.count())
      filteredRDD
    }

    //去重思路;把相同的mid的数据分成一组 ，每组取第一个
    val groupbyMidDstream: DStream[(String, Iterable[StartUpLog])] = filteredDstream.map(startuplog => (startuplog.mid, startuplog)).groupByKey()
    val distinctDstream: DStream[StartUpLog] = groupbyMidDstream.flatMap { case (mid, startulogItr) =>
      startulogItr.take(1)
    }

    // 保存到redis中
    distinctDstream.foreachRDD { rdd =>
      //driver
      // redis  type set
      // key  dau:2019-06-03    value : mids
      rdd.foreachPartition { startuplogItr =>
        //executor
        val jedis: Jedis = RedisUtil.getJedisClient
        val list: List[StartUpLog] = startuplogItr.toList
        for (startuplog <- list) {
          val key = "dau:" + startuplog.logDate
          val value = startuplog.mid
          jedis.sadd(key, value)
          println(startuplog) //
        }

        //往es中保存

      }
    }

    ssc.start()

    ssc.awaitTermination()

  }
}

