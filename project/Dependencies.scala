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
      val catsEffect = "1.0.0-RC"
    }
    lazy val cats = {
      Seq("cats-core", "cats-free")
        .map({
          case x if x == "cats-effect" ⇒ Dpd("org.typelevel", x, versions.catsEffect)
          case x                       ⇒ Dpd("org.typelevel", x, versions.catsNormal)
        })
        .map(_.libraryDependencies)
    }
    lazy val scalatest = Seq(
      Dpd("org.scalactic", "scalactic", versions.scalatest),
      Dpd("org.scalatest",
          "scalatest",
          versions.scalatest,
          autoScalaVersion = true,
          configuration = "test")
    ).map(_.libraryDependencies)

    lazy val cli = Seq(
      Dpd("bigknife.cli", "cli", versions.cli)
    ).map(_.libraryDependencies)

    lazy val sop = Seq(
      Dpd("bigknife.sop", "core", versions.sop),
      Dpd("bigknife.sop", "effect", versions.sop)
    ).map(_.libraryDependencies)
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
