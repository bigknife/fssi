import Common._, prj._
//import sbtassembly.AssemblyPlugin.defaultShellScript

coverageExcludedFiles in ThisBuild := ".*macro.*"
coverageEnabled in ThisBuild := true
parallelExecution in ThisBuild := false
// ast prj
lazy val pAst = ast()

// interpreter prj
lazy val pInterpreter = interpreter()
  .dependsOn(pAst)
  .dependsOn(pJsonRpc)

// jsonrpc prj
lazy val pJsonRpc = jsonrpc()

//sandbox prj
lazy val pSandbox = sandbox()
  .dependsOn(pContractLib)

// contract lib
lazy val pContractLib = contractLib()

// fssi
lazy val pFssi = fssi("fssi", ".")
  .dependsOn(pInterpreter)
  .dependsOn(pSandbox)
  .settings(
    mainClass in assembly := Some("fssi.world.Main"),
    assemblyMergeStrategy in assembly := {
      case "module-info.class"          => MergeStrategy.discard
      case PathList("META-INF", xs @ _) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(prependShellScript = Some(defaultShellScript)),
    assemblyJarName in assembly := s"${name.value}-${version.value}",
    test in assembly := {}
  )
