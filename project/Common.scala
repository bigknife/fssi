import sbt._, Keys._
import Dependencies._
import bintray.BintrayKeys._

object Common {

  // common settings
  lazy val settings = Seq(
    organization := "fssi",
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.11.12", "2.12.4"),
    resolvers ++= Seq(
      resolver.local,
      resolver.bigknife
    ),
    scalacOptions ++= {

      /** this is from [[https://tpolecat.github.io/2017/04/25/scalac-flags.html]] */
      Seq(
        "-deprecation", // Emit warning and location for usages of deprecated APIs.
        "-encoding",
        "utf-8", // Specify character encoding used by source files.
        "-explaintypes", // Explain type errors in more detail.
        "-feature", // Emit warning and location for usages of features that should be imported explicitly.
        "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
        "-language:experimental.macros", // Allow macro definition (besides implementation and application)
        "-language:higherKinds", // Allow higher-kinded types
        "-language:implicitConversions", // Allow definition of implicit functions called views
        "-unchecked",  // Enable additional warnings where generated code depends on assumptions.
        "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
        //"-Xfatal-warnings", // Fail the compilation if there are any warnings.
        "-Xfuture", // Turn on future language features.
        "-Xlint:adapted-args",              // Warn if an argument list is modified to match the receiver.
        "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
        //"-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
        "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
        "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
        "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
        "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
        "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
        "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
        "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
        "-Xlint:option-implicit", // Option.apply used implicit view.
        "-Xlint:package-object-classes", // Class or object defined in package object.
        "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
        "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
        "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
        "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
        "-Xlint:unsound-match", // Pattern match may not be typesafe.
        "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
        "-Ypartial-unification", // Enable partial unification in type constructor inference
        "-Ywarn-dead-code",      // Warn when dead code is identified.
        //"-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
        "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
        "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
        "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
        "-Ywarn-nullary-unit",  // Warn when nullary methods return Unit.
        "-Ywarn-numeric-widen", // Warn when numerics are widened.
        //"-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
        //"-Ywarn-unused:imports", // Warn if an import selector is not referenced.
        //"-Ywarn-unused:locals", // Warn if a local definition is unused.
        //"-Ywarn-unused:params", // Warn if a value parameter is unused.
        //"-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
        //"-Ywarn-unused:privates", // Warn if a private member is unused.
        "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
      )
    },
    scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
    addCompilerPlugin("org.scalameta"  % "paradise"        % "3.0.0-M10" cross CrossVersion.full),
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    bintrayRepository := "maven",
    libraryDependencies ++= all.scalatest,
    libraryDependencies ++= all.log
  )

  object prj {
    object fssi {
      def apply(id: String, dir: String): Project =
        Project(id, file(dir))
          .settings(settings)
          .settings(
            libraryDependencies ++= (all.sop ++ all.cli)
          )
      def apply(id: String): Project = apply(id, id)
    }

    object ast {
      def apply(id: String, dir: String): Project =
        Project(id, file(dir))
          .settings(settings)
          .settings(
            libraryDependencies ++= all.sop,
            libraryDependencies ++= all.cats
          )

      def apply(id: String): Project = apply(id, id)
      def apply(): Project           = apply("ast")
    }

    object jsonrpc {
      def apply(id: String, dir: String): Project =
        Project(id, file(dir))
          .settings(settings)
          .settings(
            libraryDependencies ++= all.cats,
            libraryDependencies ++= all.http4s,
            libraryDependencies ++= all.circe
          )

      def apply(id: String): Project = apply(id, id)
      def apply(): Project           = apply("jsonrpc")
    }

    object interpreter {
      def apply(id: String, dir: String): Project =
        Project(id, file(dir))
          .settings(settings)
          .settings(
            libraryDependencies ++= all.cats,
            libraryDependencies ++= all.bcprov,
            libraryDependencies ++= all.h2,
            libraryDependencies ++= all.circe,
            libraryDependencies ++= all.scalecube,
            libraryDependencies ++= all.betterfiles,
            libraryDependencies ++= all.leveldb,
            libraryDependencies ++= all.scapap
          )
      def apply(id: String): Project = apply(id, id)
      def apply(): Project           = apply("interpreter")
    }

    object sandbox {
      def apply(id: String, dir: String): Project =
        Project(id, file(dir))
        .settings(settings)
        .settings(
          libraryDependencies ++= all.asm
        )
      def apply(id: String): Project = apply(id, id)
      def apply(): Project           = apply("sandbox")
    }

    object contractLib {
      def apply(id: String,dir: String): Project =
        Project(id, file(dir))
        .settings(settings)
        .settings(
          version := "0.0.1_BETA"
        )
      def apply(id: String): Project = apply(id, id)
      def apply(): Project = apply("contract-lib")
    }

    object scp {
      def apply(id: String, dir: String): Project = Project(id, file(dir))
        .settings(settings)
        .settings(
          libraryDependencies ++= all.sop,
          libraryDependencies ++= all.cats
        )
      def apply(id: String): Project = apply(id, id)
      def apply(): Project = apply("scp")
    }

    object crypto {
      def apply(id: String, dir: String): Project = Project(id, file(dir))
        .settings(settings)
        .settings(
          libraryDependencies ++= all.bcprov
        )
      def apply(id: String): Project = apply(id, id)
      def apply(): Project = apply("crypto")
    }
  }

  val defaultShellScript: Seq[String] = defaultShellScript(
    Seq(
      //"--add-exports",
      //"java.base/jdk.internal.misc=ALL-UNNAMED"
    ),
    Seq(
      //"-Dio.netty.tryReflectionSetAccessible=true"
    )
  )

  def defaultShellScript(opts: Seq[String], javaOpts: Seq[String] = Seq.empty): Seq[String] = {
    val javaOptsString = javaOpts.map(_ + " ").mkString
    val optsString = opts.map(_ + " ").mkString
    Seq(
      "#!/usr/bin/env sh",
      s"""exec java $optsString -jar $javaOptsString$$JAVA_OPTS "$$0" "$$@"""",
      "")
  }
}
