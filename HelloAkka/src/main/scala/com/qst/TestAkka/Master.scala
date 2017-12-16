package com.qst.TestAkka

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}



case class SubmitTask(path:String)
case class Task(file: File )
case class Result(r:Map[String,Int])
class Master extends Actor{
  val futures=new ArrayBuffer[Future[Any]]
  val results =new ArrayBuffer[Result]
  implicit val timeout=Timeout.apply(1L,TimeUnit.HOURS)
  override def receive: Receive = {
    case SubmitTask(path)=>{
      //读取该目录下的文件
      val file=new File(path)
      val files:Array[File]=file.listFiles()
      for(f<-files;if f.isFile){
        val workerRef=context.actorOf(Props[Worker])
        val future:Future[Any]=workerRef ? Task(f)
        futures +=future
      }
      while(futures.length > 0) {
        //拿出完成计算的future
        val dones = futures.filter(_.isCompleted)
        //循环
        for(done <- dones) {
          //得到结果
          val r: Result = done.value.get.get.asInstanceOf[Result]
          results += r
          //移除已经计算好的future
          futures -= done
        }
        Thread.sleep(500)
      }
      //最后统计所有的结果
      val finalR = results.flatMap(_.r.toList).groupBy(_._1).mapValues(_.foldLeft(0)(_ + _._2))

      //将结果回传给driver
      sender() ! finalR

    }
  }
}
object Master {
  def main(args: Array[String]): Unit = {
    implicit val timeout=Timeout.apply(1L,TimeUnit.HOURS)
    val actorSystem:ActorSystem=ActorSystem("ActorSystem")
    val masterRef:ActorRef=actorSystem.actorOf(Props(new Master),"Master")
    //发送异步消息，但是返回一个Future
    val future: Future[Any] = masterRef ? SubmitTask("D:/wordCount")

    implicit val executionContextExecutor = global()
    future.foreach(r => {
      println(r)
    })

    //停掉计算任务
    actorSystem.terminate()


  }

}

