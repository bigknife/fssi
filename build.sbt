import Common._, prj._
//import sbtassembly.AssemblyPlugin.defaultShellScript

coverageExcludedFiles in ThisBuild := ".*macro.*"
coverageEnabled.in(Test, test) := true
parallelExecution in ThisBuild := false
fork in ThisBuild := true
ensimeScalaVersion in ThisBuild := "2.12.4"
ensimeIgnoreMissingDirectories in ThisBuild := true

// crypto prj
lazy val pCrypto = crypto()

// ast prj
lazy val pAst = ast()
  .dependsOn(pContractLib)

// jsonrpc prj
lazy val pJsonRpc = jsonrpc()

//sandbox prj
lazy val pSandbox = sandbox()
  .dependsOn(pContractLib)

// scp prj
//lazy val pScp = scp()
//  .dependsOn(pCrypto)

// interpreter prj
lazy val pInterpreter = interpreter()
  .dependsOn(pAst)
  .dependsOn(pJsonRpc)
  .dependsOn(pSandbox)
//  .dependsOn(pScp)

// contract lib
lazy val pContractLib = contractLib()

// fssi
lazy val pFssi = fssi("fssi", ".")
  .dependsOn(pInterpreter)
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
    assemblyJarName in assembly := s"${name.value}",
    test in assembly := {}

  )
