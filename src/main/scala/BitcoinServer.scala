/**
 * Created by mayank on 9/11/15.
 */
import akka.actor.{ActorSystem,Actor,ActorLogging,Props}
import akka.routing.RoundRobinPool
import java.security.MessageDigest
import collection.mutable.ListBuffer

case object MineCoins
case class Work(start: Int, quantity: Int, leadinZeros: Int)
case class Result(coinList: ListBuffer[Tuple2[String, String]] )



class Worker extends Actor with ActorLogging{
  def findCoins(start:Int, quantity:Int, leadingZeros: Int):ListBuffer[Tuple2[String, String]]  ={

    /*for(tuples : Tuple2[String, String] <- bitCoins){
      println(tuples._1, tuples._2)
    }*/
    return digestSHA256("kjsdfk", leadingZeros, "mayank.wadhawan", start, quantity)
  }
  def digestSHA256(input: String, zeros: Int,  gatorId: String, start: Int, quantity: Int) : ListBuffer[Tuple2[String, String]] = {
    var hasZeroes: String = ""
    var bitCoins = new ListBuffer[Tuple2[String, String]]()
    for (i <- 1 to zeros)
      hasZeroes += "0"
    for(attempts <- start to start+quantity){
      val s : String = gatorId +";" +input  + attempts.toString()
      val digest : String = MessageDigest.getInstance("SHA-256").digest(s.getBytes)
        .foldLeft("")((s: String, b: Byte) => s + Character.forDigit((b & 0xf0) >> 4, 16) +
        Character.forDigit(b & 0x0f, 16))
      if(digest.startsWith(hasZeroes))
        bitCoins += ((s, digest))
    }
    return bitCoins
  }
  def receive={
    case Work(start, quantity, leadingZeros)=>
      sender ! Result(findCoins(start, quantity, leadingZeros))
  }
}


class Master extends Actor with ActorLogging{

  val startTime=System.currentTimeMillis()
  var currentTasks:Int=_
  val noOfWorkers: Int=10
  val noOfTasks: Int=3000
  val sizeOfWork: Int=10000
  val leadingZeros=5
  var duration: Double=_
  val workerScheduler=context.actorOf(RoundRobinPool(noOfWorkers).props(Props[Worker]), "schedule")
  var coins = new ListBuffer[Tuple2[String, String]]()
  def displaySolution(coins: ListBuffer[Tuple2[String, String]]) = {
    for(coin: Tuple2[String, String]<-coins){
      println(coin._1+" bitcoin is "+coin._2)
    }
  }
  def receive={
    case MineCoins=>
      for(i<-0 to noOfTasks){
        workerScheduler ! Work(i*sizeOfWork, sizeOfWork, leadingZeros)
      }
    case Result(list)=>
      //compile
      currentTasks+=1

     for(coin:Tuple2[String, String]<-list){
       coins+=coin
     }
      if(noOfTasks==currentTasks){
        //print solution
        displaySolution(coins)
        duration=(System.currentTimeMillis()-startTime)/1000d
        println("Time taken=%s seconds".format(duration))
        context.stop(self)
        context.system.shutdown()
      }
  }
}

object BitcoinServer extends App{
  val system=ActorSystem("Bitcoin-server")
  val masterActor=system.actorOf(Props(new Master),"masterActor")
  masterActor ! MineCoins
  system.awaitTermination()
}
