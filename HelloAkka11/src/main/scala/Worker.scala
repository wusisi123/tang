

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.dispatch.ExecutionContexts.global
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import akka.pattern.ask

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

/**
  * Created by zhang on 2017/12/14.
  */
class Worker(val masterHost: String, val masterPort: Int, val memory: Int, cores: Int) extends Actor{
  var master: ActorSelection = _
  val WORKER_ID = "吴思思"
  val futures=new ArrayBuffer[Future[Any]]
  val results =new ArrayBuffer[Result]
  implicit val timeout=Timeout.apply(1L,TimeUnit.HOURS)
  override def preStart(): Unit = {

    master = context.actorSelection(s"akka.tcp://MasterSystem@192.168.31.219:8888/user/Master")
    master ! RegisterWorker(WORKER_ID, memory,cores)
  }
  override def receive: Receive = {
    case  "reply"=>{
      val future: Future[Any] = self ? SubmitTask("D:/wordCount")
      implicit val executionContextExecutor = global()
      future.foreach(r => {
        println(r)
      })
      sender() ! future
    }


    case RegisteredWorker => {
      //Worker启动一个定时器，定期向Master发送心跳
      //利用akka启动一个定时器,自己给自己发消息
      import context.dispatcher
      context.system.scheduler.schedule(0 millis, 10000 millis, self, SendHeartbeat)
      //context.system.scheduler.schedule(self,SubmitTask(file))

    }

    case SendHeartbeat => {
      //执行判断逻辑
      //向Master发送心跳
      master ! Heartbeat(WORKER_ID)

    }
    case Task(file)=>{
      val wordAndCounts:Map[String,Int]=Source.fromFile(file).getLines().map(_.split("\\|").toList(3).split("\\/").toList(3).split(" ")(0)).toList.map((_,1)).groupBy(_._1).mapValues(_.size)
      //将结果返回给发送者
      sender() ! Result(wordAndCounts.toSeq)
    }
  }
}
object Worker {

  def main(args: Array[String]): Unit = {
    val host = "192.168.31.219"
    val port = "9999".toInt
    val masterHost = "192.168.31.219"
    val masterPort = "8888".toInt
    val memory = "324".toInt
    val cores = "2".toInt
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
         """.stripMargin

    val config = ConfigFactory.parseString(configStr)
    //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
    val actorSystem = ActorSystem("WorkerSystem", config)
    //创建Actor
    actorSystem.actorOf(Props(new Worker(masterHost, masterPort, memory, cores)), "Worker")
    actorSystem.awaitTermination()
  }

}
