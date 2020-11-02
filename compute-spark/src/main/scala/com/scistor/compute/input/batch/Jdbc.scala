package com.scistor.compute.input.batch

import java.util
import java.util.Properties

import com.scistor.compute.apis.BaseStaticInput
import com.scistor.compute.model.remote.TransStepDTO
import org.apache.spark.sql.{Dataset, Row, SparkSession}

import scala.collection.JavaConversions._

class Jdbc extends BaseStaticInput {

  var config: TransStepDTO = _

  var extraProp: Properties = new Properties

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
    val attrs = config.getStepAttributes
    if (!attrs.containsKey("connectUrl")) {
      (false, s"please specify [connectUrl] in ${attrs.getOrElse("dataSourceType", "")} as a non-empty string")
    } else {
      (true, "")
    }
  }

  def initProp(driver: String): Tuple2[Properties, Array[String]] = {
    val attrs = config.getStepAttributes
    val definedProps = attrs.get("properties").asInstanceOf[util.Map[String, AnyRef]]
    val prop = new Properties()
    prop.setProperty("driver", driver)
    prop.setProperty("user", definedProps.get("user").toString)
    prop.setProperty("password", definedProps.get("password").toString)

    (prop, new Array[String](0))
  }

  def jdbcReader(sparkSession: SparkSession, driver: String): Dataset[Row] = {
    val attrs = config.getStepAttributes
    var dataframe: Dataset[Row] = null
    val tuple = initProp(driver)
    if (tuple._2 != null && tuple._2.length > 0) {
      tuple._2.map(x => {
        println(s"source predicates: $x")
      })
      dataframe = sparkSession.read.jdbc(attrs.get("connectUrl").toString, attrs.get("source").toString, tuple._2, tuple._1)
    } else {
      dataframe = sparkSession.read.jdbc(attrs.get("connectUrl").toString, attrs.get("source").toString, tuple._1)
    }

    dataframe
  }

  override def getDataset(spark: SparkSession): Dataset[Row] = {
    jdbcReader(spark, "com.mysql.jdbc.Driver")
  }
}
