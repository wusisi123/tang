package com.qst.TestAkka

import akka.actor.Actor

import scala.io.Source

class Worker extends Actor{
  override def receive: Receive = {
    case Task(file)=>{
      val wordAndCounts:Map[String,Int]=Source.fromFile(file).getLines().map(_.split("\\|").toList(3).split("\\/").toList(3).split(" ")(0)).toList.map((_,1)).groupBy(_._1).mapValues(_.size)
      //将结果返回给发送者
      sender() ! Result(wordAndCounts)
    }
  }
}
