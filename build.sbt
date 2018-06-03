import Common._, prj._
//import sbtassembly.AssemblyPlugin.defaultShellScript

coverageExcludedFiles in ThisBuild := ".*macro.*"
coverageEnabled in ThisBuild := true
// ast prj
lazy val pAst = ast()

// interpreter prj
lazy val pInterpreter = interpreter()
  .dependsOn(pAst)

// jsonrpc prj
lazy val pJsonRpc = jsonrpc()

// fssi
lazy val pFssi = fssi("fssi", ".")
  .dependsOn(pInterpreter)
  .dependsOn(pJsonRpc)
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
