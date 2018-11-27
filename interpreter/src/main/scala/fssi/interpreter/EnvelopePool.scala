package fssi.interpreter

import fssi.base.Var
import fssi.interpreter.scp.SCPEnvelope
import fssi.scp.interpreter.NodeStoreHandler
import fssi.scp.types.{Message, NodeID}
import org.slf4j.LoggerFactory

trait EnvelopePool {
  private val nomPool: Var[Map[String, Vector[SCPEnvelope]]] = Var(Map.empty)
  private val ballotPool: Var[Map[String, Vector[SCPEnvelope]]] = Var(Map.empty)

  private val workingNom: Var[Map[String,Option[SCPEnvelope]]] = Var(Map.empty)
  private val workingBallot: Var[Map[String, Option[SCPEnvelope]]] = Var(Map.empty)

  private lazy val log = LoggerFactory.getLogger(getClass)

  def put(envelope: SCPEnvelope): Unit = {
    val key = envelope.value.statement.from.toString
    envelope.value.statement.message match {
      case x: Message.Nomination =>
        val m = nomPool.unsafe()
        if (m.contains(key)) {
          // find if any element >= self
          val isOld = m(key).exists {x0 =>
            x0.value.statement.message match {
              case x1: Message.Nomination =>
                x1.isNewerThan(x)
              case _ => false
            }
          }

          if (!isOld) nomPool.update {map =>
            val ov = map(key)
            val filtered = ov.filter(y => workingNom.map{z =>
              z.get(key).exists(_.contains(y))
            }.unsafe())
            map + (key -> (filtered :+ envelope))
          }
          else ()
        }
        else nomPool.update(_ + (key -> Vector(envelope)))

      case x: Message.BallotMessage =>
        val m = ballotPool.unsafe()
        if(m.contains(key)) {
          // find if any element >= self
          val isOld = m(key).exists {x0 =>
            NodeStoreHandler.instance.isNewerBallotMessage(x0.value.statement.message.asInstanceOf[Message.BallotMessage], x)
          }
          if (!isOld) ballotPool.update {map =>
            val ov = map(key)
            val filtered = ov.filter(y => workingBallot.map{z =>
              z.get(key).exists(_.contains(y))
            }.unsafe())
            map + (key -> (filtered :+ envelope))
          }
          else ()
        }
        else ballotPool.update(_ + (key -> Vector(envelope)))
    }

    nomPool.foreach {map =>
       map.mapValues(_.size).foreach {
         case (nodeId, size) => log.error(s"nomPool: ${nodeId.take(4)}-$size")
       }
    }
    ballotPool.foreach {map =>
      map.mapValues(_.size).foreach {
        case (nodeId, size) => log.error(s"ballotPool: ${nodeId.take(4)}-$size")
      }
    }
  }

  def getUnworkingNom(nodeId: NodeID): Option[SCPEnvelope] = {
    val key = nodeId.toString
    val r = nomPool.map(_.get(nodeId.toString)).unsafe().flatMap {envelopes =>
      val xs = envelopes.filter(x => workingNom.map{y =>
        !y.get(key).exists(_.contains(x))
      }.unsafe())
      if (xs.isEmpty) None
      else if (xs.length == 1) xs.headOption
      else {
        val sorted = xs.sortWith {
          case (x0, x1) =>
            val n0 = x0.value.statement.message.asInstanceOf[Message.Nomination]
            val n1 = x1.value.statement.message.asInstanceOf[Message.Nomination]
            n1.isNewerThan(n0)
        }
        sorted.lastOption
      }
    }

    log.error(s"working ballot: ${nodeId.toString.take(4)}-${r.map(x => x.hashCode())}")

    r
  }

  def getUnworkingBallot(nodeId: NodeID): Option[SCPEnvelope] = {
    val key = nodeId.toString
    val r = ballotPool.map(_.get(nodeId.toString)).unsafe().flatMap {envelopes =>
      val xs = envelopes.filter(x => workingBallot.map{y =>
        !y.get(key).exists(_.contains(x))
      }.unsafe())
      if (xs.isEmpty) None
      else if (xs.length == 1) xs.headOption
      else {
        val sorted = xs.sortWith {
          case (x0, x1) =>
            val n0 = x0.value.statement.message.asInstanceOf[Message.BallotMessage]
            val n1 = x1.value.statement.message.asInstanceOf[Message.BallotMessage]
            NodeStoreHandler.instance.isNewerBallotMessage(n1, n0)
        }
        sorted.lastOption
      }
    }

    log.error(s"working ballot: ${nodeId.toString.take(4)}-${r.map(x => x.hashCode())}")

    r
  }

  def setWorkingBallot(nodeId: NodeID, envelope: SCPEnvelope): Unit = {
    workingBallot.update {m =>
      m + (nodeId.toString -> Some(envelope))
    }
  }

  def setWorkingNom(nodeID: NodeID, envelope: SCPEnvelope): Unit = {
    workingNom.update {m =>
      m + (nodeID.toString -> Some(envelope))
    }
  }

  def endWorkingBallot(nodeId: NodeID, envelope: SCPEnvelope): Unit = {
    workingBallot.update(_ - nodeId.toString)
    nomPool.update {m =>
      val xs = m.get(nodeId.toString)
      if (xs.isDefined) {
        m + (nodeId.toString ->  xs.get.filter(_ != envelope))
      }
      else m
    }

    log.error(s"end working ballot: ${nodeId.toString.take(4)}-${envelope.hashCode()}")
  }

  def endWorkingNom(nodeID: NodeID, envelope: SCPEnvelope): Unit = {
    workingNom.update(_ - nodeID.toString)
    ballotPool.update {m =>
      val xs = m.get(nodeID.toString)
      if (xs.isDefined) {
        m + (nodeID.toString ->  xs.get.filter(_ != envelope))
      }
      else m
    }
    log.error(s"end working nom: ${nodeID.toString.take(4)}-${envelope.hashCode()}")
  }
}

object EnvelopePool extends EnvelopePool