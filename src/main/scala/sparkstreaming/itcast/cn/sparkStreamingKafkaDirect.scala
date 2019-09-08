package sparkstreaming.itcast.cn

import kafka.serializer.StringDecoder
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
//import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object sparkStreamingKafkaDirect {
  def main(args: Array[String]): Unit = {
    //1、创建sparkConf
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("SparkStreamingKafka_Direct")
//      .setMaster("local[2]")
    //2、创建sparkContext
    val sc = new SparkContext(sparkConf)
    sc.setLogLevel("WARN")
    //3、创建StreamingContext
    val ssc = new StreamingContext(sc, Seconds(5))
    ssc.checkpoint("hdfs://192.168.192.134:8020/Kafka_Direct_huahua")
    //4、配置kafka相关参数
    val kafkaParams =
      Map("metadata.broker.list" -> "linux1:9092,linux2:9092,linux3:9092",
        "group.id" -> "Kafka_Direct")
    //5、定义topic
    val topics = Set("sparkafka__12")
    //6、通过 KafkaUtils.createDirectStream接受kafka数据，这里采用是kafka低级api偏移量不受zk管理
    val dstream: InputDStream[(String, String)] =
      KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics)
    //7、获取kafka中topic中的数据
    val topicData: DStream[String] = dstream.map(_._2)
    //8、切分每一行,每个单词计为1
    val wordAndOne: DStream[(String, Int)] = topicData.flatMap(_.split(" ")).map((_, 1))
    //9、相同单词出现的次数累加
    val result: DStream[(String, Int)] = wordAndOne.reduceByKey(_ + _)
    //10、打印输出
    result.print()
    //开启计算
    ssc.start()
    ssc.awaitTermination()
  }
}
