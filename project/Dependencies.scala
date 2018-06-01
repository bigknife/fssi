/**
  * The definition of *ALL Dependencies*
  */
import sbt._, Keys._
import bintray.BintrayKeys._

object Dependencies {
  case class Dpd(groupId: String,
                 artifactId: String,
                 revision: String,
                 autoScalaVersion: Boolean = true,
                 configuration: String = "compile") {
    def libraryDependencies =
      if (autoScalaVersion) groupId %% artifactId % revision % configuration
      else groupId                  % artifactId  % revision % configuration
  }

  // all dependencies
  object all {
    object versions {
      val scalatest  = "3.0.5"
      val cli        = "1.0.0"
      val sop        = "1.0.0"
      val catsNormal = "1.0.1"
      val catsEffect = "0.10.1"
      val http4s     = "0.18.12"
      val circe      = "0.9.3"

      // log
      object log {
        val slf4j = "1.8.0-beta1"
        val logback = "1.3.0-alpha4"
        val logbackColorizer = "1.0.1"
      }

      // crypto
      val bcprov = "1.59"

      // builtin db h2
      val h2 = "1.4.197"
    }

    lazy val log = {
      Seq(
        Dpd("org.slf4j", "slf4j-api", versions.log.slf4j, autoScalaVersion = false),
        //Dpd("org.slf4j", "slf4j-simple", version.log.slf4j, autoScalaVersion = false, configuration = "test"),
        Dpd("ch.qos.logback", "logback-classic", versions.log.logback, autoScalaVersion = false),
        Dpd("ch.qos.logback", "logback-core", versions.log.logback, autoScalaVersion = false),
        Dpd("org.tuxdude.logback.extensions", "logback-colorizer", versions.log.logbackColorizer, autoScalaVersion = false)
      ).map(_.libraryDependencies)
    }

    lazy val cats = {
      Seq("core", "free", "effect")
        .map({
          case x if x == "effect" ⇒ Dpd("org.typelevel", s"cats-$x", versions.catsEffect)
          case x                       ⇒ Dpd("org.typelevel", s"cats-$x", versions.catsNormal)
        })
        .map(_.libraryDependencies)
    }
    lazy val scalatest = Seq(
      Dpd("org.scalactic", "scalactic", versions.scalatest),
      Dpd("org.scalatest",
          "scalatest",
          versions.scalatest,
          configuration = "test")
    ).map(_.libraryDependencies)

    lazy val cli = Seq(
      Dpd("bigknife.cli", "cli", versions.cli)
    ).map(_.libraryDependencies)

    lazy val sop = Seq(
      Dpd("bigknife.sop", "core", versions.sop),
      Dpd("bigknife.sop", "effect", versions.sop)
    ).map(_.libraryDependencies)

    lazy val http4s = Seq("dsl", "blaze-server", "circe")
      .map(x => Dpd("org.http4s", s"http4s-$x", versions.http4s))
      .map(_.libraryDependencies)

    lazy val circe = Seq("core", "generic", "parser")
      .map(x => Dpd("io.circe", s"circe-$x", versions.circe))
      .map(_.libraryDependencies)

    lazy val bcprov = {
      Seq(
        Dpd("org.bouncycastle", "bcprov-jdk15on", versions.bcprov, autoScalaVersion = false)
      ).map(_.libraryDependencies)
    }

    lazy val h2 = {
      Seq(
        Dpd("com.h2database", "h2", versions.h2, autoScalaVersion = false)
      ).map(_.libraryDependencies)
    }
  }

  // resolvers
  object resolver {
    lazy val bigknife = "bigknife bintray maven" at "https://dl.bintray.com/bigknife/maven"
    lazy val local    = "local maven repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
    object credential {
      lazy val jfrog = Credentials(Path.userHome / ".dev" / "jfrog.credentials")
    }
  }
}
