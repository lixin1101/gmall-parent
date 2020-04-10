package com.lx.utils

import java.io.InputStreamReader
import java.util.Properties

object PropertiesUtil {


  def main(args: Array[String]): Unit = {
    val properties: Properties = PropertiesUtil.load("config.properties")
    /*var tel = "15910305452"
    val t: (String, String) = tel.splitAt(4)
    val str: String = t._1 + "*****"
    println(str)*/

    println(properties.getProperty("kafka.broker.list"))
  }

  def load(propertieName: String): Properties = {
    val prop = new Properties()
    prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader.getResourceAsStream(propertieName), "UTF-8"))
    prop
  }

}
