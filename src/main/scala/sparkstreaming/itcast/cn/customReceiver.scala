package sparkstreaming.itcast.cn

import java.io.{BufferedReader, InputStreamReader}
import java.net.Socket
import java.nio.charset.StandardCharsets

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.streaming.{Seconds, StreamingContext}

object customReceiver {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
//    val sparkContext = new SparkContext(conf)
    //设置日志输出级别
//    sparkContext.setLogLevel("WARN")
    val ssc = new StreamingContext(conf, Seconds(1))

    // Create a DStream that will connect to hostname:port, like localhost:9999
    val lines = ssc.receiverStream(new customReceiver("linux1", 9999))

    // Split each line into words
    val words = lines.flatMap(_.split(" "))
    val pairs = words.map(word => (word, 1))
    val wordCounts = pairs.reduceByKey(_ + _)
    wordCounts.print()
    ssc.start()             // Start the computation
    ssc.awaitTermination()

  }

  class customReceiver(host:String,port:Int)extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2){
    override def onStart(): Unit = {
      new Thread("Socket Receiver") {
        override def run() { receive() }
      }.start()
    }

    override def onStop(): Unit = {

    }
    private def receive() {
      var socket: Socket = null
      var userInput: String = null
      try {
        // Connect to host:port
        socket = new Socket(host, port)

        // Until stopped or connection broken continue reading
        val reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))

        userInput = reader.readLine()
        while(!isStopped && userInput != null) {

          // 传送出来
          store(userInput)

          userInput = reader.readLine()
        }
        reader.close()
        socket.close()

        // Restart in an attempt to connect again when server is active again
        restart("Trying to connect again")
      } catch {
        case e: java.net.ConnectException =>
          // restart if could not connect to server
          restart("Error connecting to " + host + ":" + port, e)
        case t: Throwable =>
          // restart if there is any other error
          restart("Error receiving data", t)
      }
    }
  }
}
