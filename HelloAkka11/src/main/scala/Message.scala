import java.io.File

/**
  * Created by zhang on 2017/12/14.
  */
//worker -> master的注册消息
case class RegisterWorker(workerId: String, memory: Int, cores: Int) extends Serializable

//worker -> master的心跳信息
case class Heartbeat(workerId: String) extends Serializable


//master -> worker注册成功的消息
case object RegisteredWorker extends Serializable

//Worker -> self
case object SendHeartbeat

//Master -> self
case object CheckTimeOutWorker

case class SubmitTask(path:String)

//worker  运行wordcount
case class Task(file:File)  extends Serializable

case class Result(r:Seq[(String,Int)]) extends Serializable