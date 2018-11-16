package fssi
package interpreter
import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import fssi.types.biz._

import scala.collection.JavaConverters._
import fssi.base._
import fssi.scp.types.QuorumSet.Slices
import fssi.scp.types.{NodeID, QuorumSet}

case class ConfigReader(configFile: File) {
  private lazy val config: Config = ConfigFactory.parseFile(configFile)

  def chainId: String = config.getString("chainId")

  object core {
    private val corePrefix: String = "core-node"
    def mode: String               = config.getString(s"$corePrefix.mode")

    object consensus {
      private val consensusPrefix: String = s"$corePrefix.consensus-network"
      def host: String                    = config.getString(s"$consensusPrefix.host")
      def port: Int                       = config.getInt(s"$consensusPrefix.port")
      def seeds: Vector[Node.Addr]        = getSeeds(config.getConfig(consensusPrefix))

      def account: Account = getAccount(config.getConfig(s"$consensusPrefix.account"))

      object scp {
        val scpPrefix = s"$consensusPrefix.scp"

        def quorums: QuorumSet = {
          val threshold: Int = config.getInt(s"$scpPrefix.quorums.threshold")
          val validators: Vector[NodeID] = getValidators(
            config
              .getStringList(s"$scpPrefix.quorums.validators")
              .asScala
              .toVector)
          val innerSets: Vector[Slices.Flat] =
            config.getConfigList(s"$scpPrefix.quorums.innerSets").asScala.toVector.map {
              flatConfig =>
                val innerThreshold: Int = flatConfig.getInt("threshold")
                val innerValidators: Vector[NodeID] =
                  getValidators(flatConfig.getStringList("validators").asScala.toVector)
                Slices.Flat(innerThreshold, innerValidators)
            }
          QuorumSet.slices(Slices.nest(threshold, validators, innerSets: _*))
        }

        def maxTimeoutSeconds: Long = config.getLong(s"$scpPrefix.maxTimeoutSeconds")
        def maxNominatingTimes: Int = config.getInt(s"$scpPrefix.maxNominatingTimes")

        private def getValidators(validators: Vector[String]): Vector[NodeID] =
          validators
            .map(Base58.decode)
            .filter(_.isDefined)
            .map(_.get)
            .map(NodeID(_))
      }
    }

    object application {
      private val applicationPrefix: String = s"$corePrefix.application-network"
      def host: String                      = config.getString(s"$applicationPrefix.host")
      def port: Int                         = config.getInt(s"$applicationPrefix.port")
      def seeds: Vector[Node.Addr]          = getSeeds(config.getConfig(applicationPrefix))
      def account: Account                  = getAccount(config.getConfig(s"$applicationPrefix.account"))
    }
  }

  object edge {
    private val edgePrefix: String = "edge-node"

    object client {
      private val clientPrefix: String = s"$edgePrefix.client-network"
      object jsonRPC {
        private val jsonRPCPrefix: String = s"$clientPrefix.http-json-rpc"
        def host: String                  = config.getString(s"$jsonRPCPrefix.host")
        def port: Int                     = config.getInt(s"$jsonRPCPrefix.port")
      }
    }

    object application {
      private val applicationPrefix: String = s"$edgePrefix.application-network"
      def host: String                      = config.getString(s"$applicationPrefix.host")
      def port: Int                         = config.getInt(s"$applicationPrefix.port")
      def seeds: Vector[Node.Addr]          = getSeeds(config.getConfig(applicationPrefix))
      def account: Account                  = getAccount(config.getConfig(s"$applicationPrefix.account"))
    }
  }

  private def getAccount(accountConfig: Config): Account = {
    val idOpt =
      Base58.decode(accountConfig.getString(s"id"))
    require(idOpt.nonEmpty, "account id must encode by base58")
    val id           = Account.ID(idOpt.get)
    val publicKeyOpt = Base58.decode(accountConfig.getString(s"publicKey"))
    require(publicKeyOpt.nonEmpty, "account public key must encode by base58")
    val publicKey = Account.PubKey(publicKeyOpt.get)
    val encPriKeyOpt =
      Base58.decode(accountConfig.getString(s"encryptedPrivateKey"))
    require(encPriKeyOpt.nonEmpty, "account encrypt private key must encode by base58")
    val encPriKey = Account.PrivKey(encPriKeyOpt.get)
    val ivOpt     = Base58.decode(accountConfig.getString(s"iv"))
    require(ivOpt.nonEmpty, "account iv must encode by base58")
    val iv = Account.IV(ivOpt.get)
    Account(encPriKey, publicKey, iv, id)
  }

  private def getSeeds(seedsConfig: Config): Vector[Node.Addr] =
    seedsConfig
      .getStringList("seeds")
      .asScala
      .toVector
      .map(Node.parseAddr)
      .filter(_.isDefined)
      .map(_.get)
}
