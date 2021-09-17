package com.lnvortex.server.config

import akka.actor.ActorSystem
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import org.bitcoins.commons.config._
import org.bitcoins.core.currency.Satoshis
import org.bitcoins.core.util.FutureUtil
import org.bitcoins.crypto._
import org.bitcoins.db._
import org.bitcoins.tor.config.TorAppConfig
import org.bitcoins.tor.{Socks5ProxyParams, TorParams}

import java.net.{InetSocketAddress, URI}
import java.nio.file.{Path, Paths}
import java.time.Duration
import scala.concurrent._
import scala.util.Properties

/** Configuration for Ln Vortex
  *
  * @param directory The data directory of the wallet
  * @param conf      Optional sequence of configuration overrides
  */
case class VortexCoordinatorAppConfig(
    private val directory: Path,
    private val conf: Config*)(implicit system: ActorSystem)
    extends DbAppConfig
    with JdbcProfileComponent[VortexCoordinatorAppConfig]
    with DbManagement
    with Logging {
  import system.dispatcher

  override val configOverrides: List[Config] = conf.toList
  override val moduleName: String = VortexCoordinatorAppConfig.moduleName
  override type ConfigType = VortexCoordinatorAppConfig

  override val appConfig: VortexCoordinatorAppConfig = this

  import profile.api._

  override def newConfigOfType(
      configs: Seq[Config]): VortexCoordinatorAppConfig =
    VortexCoordinatorAppConfig(directory, configs: _*)

  override val baseDatadir: Path = directory

  override def start(): Future[Unit] = FutureUtil.unit

  override def stop(): Future[Unit] = Future.unit

  lazy val torConf: TorAppConfig =
    TorAppConfig(directory, conf: _*)

  lazy val socks5ProxyParams: Option[Socks5ProxyParams] =
    torConf.socks5ProxyParams

  lazy val torParams: Option[TorParams] = torConf.torParams

  lazy val listenAddress: InetSocketAddress = {
    val str = config.getString(s"$moduleName.listen")
    val uri = new URI("tcp://" + str)
    new InetSocketAddress(uri.getHost, uri.getPort)
  }

  lazy val mixAmount: Satoshis = {
    val long = config.getLong(s"$moduleName.mixAmount")
    Satoshis(long)
  }

  lazy val mixFee: Satoshis = {
    val long = config.getLong(s"$moduleName.mixFee")
    Satoshis(long)
  }

  lazy val interval: Duration = {
    config.getDuration(s"$moduleName.mixInterval")
  }

  lazy val seedPath: Path = directory.resolve("seed.json")

  lazy val aesPasswordOpt: Option[AesPassword] = {
    val passOpt = config.getStringOrNone(s"bitcoin-s.keymanager.aesPassword")
    passOpt.flatMap(AesPassword.fromStringOpt)
  }

  lazy val bip39PasswordOpt: Option[String] = {
    config.getStringOrNone(s"bitcoin-s.keymanager.bip39password")
  }

  override lazy val allTables: List[TableQuery[Table[_]]] = List.empty
}

object VortexCoordinatorAppConfig
    extends AppConfigFactoryBase[VortexCoordinatorAppConfig, ActorSystem] {
  override val moduleName: String = "vortex"

  val DEFAULT_DATADIR: Path = Paths.get(Properties.userHome, ".ln-vortex")

  override def fromDefaultDatadir(confs: Vector[Config] = Vector.empty)(implicit
      ec: ActorSystem): VortexCoordinatorAppConfig = {
    fromDatadir(DEFAULT_DATADIR, confs)
  }

  override def fromDatadir(datadir: Path, confs: Vector[Config])(implicit
      ec: ActorSystem): VortexCoordinatorAppConfig =
    VortexCoordinatorAppConfig(datadir, confs: _*)
}