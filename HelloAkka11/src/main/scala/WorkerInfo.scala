

/**
  * Created by zhang on 2017/12/14.
  */

class WorkerInfo(val workerId: String, var memory: Int, var cores: Int) {

  var lastHeartbeatTime: Long = _
}

object WorkerInfo {

}
