package com.scistor.compute.output.batch

import java.util
import java.util.Properties

import com.scistor.compute.apis.BaseOutput
import com.scistor.compute.model.remote.TransStepDTO
import org.apache.spark.sql.{Dataset, Row, SparkSession}

import scala.collection.JavaConversions._

class Mysql extends BaseOutput {

  var config: TransStepDTO = _

  /**
   * Set Config.
   **/
  override def setConfig(config: TransStepDTO): Unit = {
    this.config = config
  }

  /**
   * Get Config.
   **/
  override def getConfig(): TransStepDTO = config

  /**
   * Return true and empty string if config is valid, return false and error message if config is invalid.
   */
  override def validate(): (Boolean, String) = {
    (true, "")
  }

  override def prepare(spark: SparkSession): Unit = {
    super.prepare(spark)
  }

  override def process(df: Dataset[Row]): Unit = {
    val attrs = config.getStepAttributes
    val prop = new Properties()

    val definedProps = attrs.get("properties").asInstanceOf[util.Map[String, AnyRef]]
    for ((k, v) <- definedProps) {
      prop.setProperty(k, v.toString)
    }
    prop.setProperty("driver", "com.mysql.cj.jdbc.Driver")

    println(s"[INFO] 输出数据源 <${config.getStepType}> properties: ")
    prop.foreach(entry => {
      val (key, value) = entry
      println("\t" + key + " = " + value)
    })

    val saveMode = definedProps.getOrElse("saveMode", "append").toString

    df.write.mode(saveMode).jdbc(attrs.get("connectUrl").toString, attrs.get("source").toString, prop)

  }

}
