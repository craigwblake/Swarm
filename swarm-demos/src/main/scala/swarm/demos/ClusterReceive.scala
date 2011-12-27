package swarm.demos

import swarm.jgroups.JGroupsTransporter

object ClusterReceive {
    def main (args: Array[String]) = {
        implicit val transporter = JGroupsTransporter
    }
}
