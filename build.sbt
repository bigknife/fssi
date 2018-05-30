import Common._, prj._
import sbtassembly.AssemblyPlugin.defaultShellScript

coverageExcludedFiles in ThisBuild := ".*macro.*"
// ast prj
lazy val pAst = ast("ast")

// fssi
lazy val pFssi = fssi("fssi", ".")
  .dependsOn(pAst)
  .settings(
    mainClass in assembly := Some("fssi.world.Main"),
    assemblyMergeStrategy in assembly := {
      case "module-info.class" => MergeStrategy.discard
      case PathList("META-INF", xs@_) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(prependShellScript = Some(defaultShellScript)),
    assemblyJarName in assembly := s"${name.value}-${version.value}",
    test in assembly := {}
  )
