package swarm.jgroups

import java.io.{InputStream,OutputStream}
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.atomic.AtomicBoolean
import org.jgroups.{Address,Channel,JChannel,Message,ReceiverAdapter,TimeoutException,View}
import scala.collection.JavaConversions._
import scala.collection.mutable.{HashSet,SynchronizedSet}
import scala.util.Random.nextInt
import swarm.{Bee,Logs}
import swarm.Swarm.continue
import swarm.transport.{Location,Transporter}

case class ClusterLocation (address: Option[Address] = None) extends Location

object JGroupsTransporter extends ReceiverAdapter with Transporter with Logs {

    implicit def func2callable[T] (f: () => T): Callable[T] = new Callable[T] {
        override def call: T = f()
    }

    val executor = newCachedThreadPool

    val disabled = new AtomicBoolean()
    val suspected = new HashSet[Address] with SynchronizedSet[Address]

    val channel = new JChannel("udp.xml")
    channel.setReceiver(this)
    channel.connect("swarm")

    var address = channel.getAddress

    def isLocal (location: Location): Boolean = location match {
        case location: ClusterLocation => location.address.equals(address)
        case _ => false
    }

    def members: Seq[ClusterLocation] = channel.getView.getMembers.map(x => ClusterLocation(Some(x)))

    def randomMember: ClusterLocation = {
        val locations = members
        locations(nextInt(locations.size))
    }

    def transport (f: (Unit => Bee), destination: Location): Unit = destination match {
        case destination: ClusterLocation =>
            if (disabled.get) throw new RuntimeException("node is blocked, cannot send")

            val address = destination.address match {
                case Some(address) => address
                case None => randomMember.address.get
            }

            val message = new Message(address, f)
            message.setFlag(Message.RSVP)

            try {
                channel.send(message)
            } catch {
                case e =>
                    println("send error, attempting redelivery")
                    transport(f, destination)
            }
    }

    override def receive (message: Message) = {
        val bee = message.getObject.asInstanceOf[(Unit => Bee)]
        val f: () => Unit = () => { continue(bee)(this) }
        executor.submit(f)
    }

    override def unblock = disabled.set(false)

    override def block = disabled.set(true)

    override def suspect (address: Address) = suspected += address

    override def viewAccepted (view: View) = suspected.retain(!view.containsMember(_))
}
