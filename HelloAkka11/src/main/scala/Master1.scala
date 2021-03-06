import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

class Master1 extends Actor {
  val futures = new ArrayBuffer[Future[Any]]
  val results = new ArrayBuffer[Result]
  implicit val timeout = Timeout.apply(1L, TimeUnit.HOURS)
  val id2Workers = new mutable.HashMap[String, WorkerInfo]()

  val CHECK_INTERVAL = 15000
  //Master创建之后就启动一个定时器，用来检测超时的Worker
  override def preStart(): Unit = {
    //导入隐式转换
    import context.dispatcher
    context.system.scheduler.schedule(0 millis, CHECK_INTERVAL millis, self, CheckTimeOutWorker)
  }

  override def receive: Receive = {
    case "hello"=>{
      println("Master 管理")
    }
    case "connect" => {
      print("a client connected")
      sender ! "reply"
    }
    //Worker发送给Master的注册消息
    case RegisterWorker(workerId, memory, cores) => {
      //将注册消息保存起来
      val workerInfo = new WorkerInfo(workerId, memory, cores)
      //保存到集合
      id2Workers(workerId) = workerInfo
      implicit val executionContextExecutor = global()
      //返回一个一个消息告诉Worker注册成功了
      println(workerId)
      sender() ! RegisteredWorker
      val future: Future[Any] =  sender() ? SubmitTask("D:/wordCount")
      future.foreach(r => {
        println(r)
      })
      }



    //Worker发送给Master的心跳信息
    case Heartbeat(workerId) => {
      //根据workerId到保存worker信息的map中查找
      if (id2Workers.contains(workerId)) {
        val workerInfo: WorkerInfo = id2Workers(workerId)
        //更新Worker的状态（上一次心跳的时间）
        val current = System.currentTimeMillis()
        workerInfo.lastHeartbeatTime = current
      }
    }

    case CheckTimeOutWorker => {
      //
      val current = System.currentTimeMillis();
      //过滤出超时的Worker
      val deadWorkers = id2Workers.values.filter(w => current - w.lastHeartbeatTime > CHECK_INTERVAL)
      //移除超时的worker
      //for(w <- deadWorkers) {
      //  id2Workers -= w.workerId
      //}
      deadWorkers.foreach(dw => {
        id2Workers -= dw.workerId
      })
      println("current works size : " + id2Workers.size)
    }


  }
}

object Master1 {

  val MASTER_SYSTEM = "MasterSystem"
  val MASTER_NAME = "Master"


  def main(args: Array[String]): Unit = {
    implicit val timeout=Timeout.apply(1L,TimeUnit.HOURS)
    val host ="192.168.31.219"
    val port = "8888".toInt

    //创建一个配置文件字符串(ip,port)
    val confStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = $host
         |akka.remote.netty.tcp.port = $port
      """.stripMargin

    val conf = ConfigFactory.parseString(confStr)
    //创建ActorSystem，指定了其配置信息（ip、端口、通信的实现类）
    val masterActorSystem = ActorSystem(MASTER_SYSTEM, conf)
    //通过ActorSystem创建Actor
    val masterRef:ActorRef= masterActorSystem.actorOf(Props[Master1], MASTER_NAME)
    //发送异步消息，但是返回一个Future
    /*val future: Future[Any] = masterRef ? SubmitTask("D:/wordCount")

    implicit val executionContextExecutor = global()


    //停掉计算任务
    //masterActorSystem.terminate()*/
    masterRef ! "hello"
    masterActorSystem.whenTerminated

  }
}
