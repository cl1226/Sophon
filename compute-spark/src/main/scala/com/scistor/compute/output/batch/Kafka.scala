package com.scistor.compute.output.batch

import java.util.Properties

import com.scistor.compute.apis.BaseOutput
import com.scistor.compute.model.remote.TransStepDTO
import com.scistor.compute.output.utils.KafkaProducerUtil
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Dataset, Row, SparkSession}

import scala.collection.JavaConversions._

class Kafka extends BaseOutput {

  val producerPrefix = "producer."

  var kafkaSink: Option[Broadcast[KafkaProducerUtil]] = None

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

  /**
   * Prepare before running, do things like set config default value, add broadcast variable, accumulator.
   */
  override def prepare(spark: SparkSession): Unit = {
    super.prepare(spark)
    val attrs = config.getStepAttributes

    val props = new Properties()
    props.setProperty("format", "json")
    props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.setProperty("bootstrap.servers", attrs.get("serverAddress").toString)
    props.setProperty("group.id", attrs.get("groupid").toString)

    if (attrs.containsKey("kerberos") && attrs.get("kerberos").toString.equals("true")){
      props.setProperty("security.protocol", "SASL_PLAINTEXT")
      props.setProperty("sasl.kerberos.service.name", "kafka")
    }

    println("[INFO] Kafka Output properties: ")
    props.foreach(entry => {
      val (key, value) = entry
      println("\t" + key + " = " + value)
    })

    kafkaSink = Some(spark.sparkContext.broadcast(KafkaProducerUtil(props)))
  }

  override def process(df: Dataset[Row]): Unit = {
    val attrs = config.getStepAttributes
    val topic = attrs.get("topic").toString
    val format = attrs.get("dataFormatType").toString
    format match {
      case "text" => {
        if (df.schema.size != 1) {
          throw new Exception(
            s"Text data source supports only a single column," +
              s" and you have ${df.schema.size} columns.")
        } else {
          df.foreach { row =>
            kafkaSink.foreach { ks =>
              ks.value.send(topic, row.getAs[String](0))
            }
          }
        }
      }
      case _ => {
        val dataSet = df.toJSON
        dataSet.foreach { row =>
          kafkaSink.foreach { ks =>
            ks.value.send(topic, row)
          }
        }
      }
    }
  }
}
