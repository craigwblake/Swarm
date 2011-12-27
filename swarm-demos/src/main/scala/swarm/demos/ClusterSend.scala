package swarm.demos

import java.net.InetAddress
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random.nextInt
import swarm._
import swarm.Swarm._
import swarm.transport._
import swarm.jgroups.{ClusterLocation,JGroupsTransporter}

object ClusterSend {

    implicit val transporter = JGroupsTransporter

    def main (args: Array[String]) = {
        spawn(ping)
    }

    def ping: Bee@swarm = {

        var counter = 0L

        while (true) {
            counter = counter + 1
            print(counter + " waiting... ")
            Thread.sleep(500)

            println("done, sending to cluster")
            moveTo(ClusterLocation())
        }

        NoBee()
    }
}
