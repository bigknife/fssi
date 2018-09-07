import Common._, prj._

coverageExcludedFiles in ThisBuild := ".*macro.*"
parallelExecution in ThisBuild := false
fork in ThisBuild := true
scalaVersion in ThisBuild := "2.12.4"
coverageEnabled in (Test, test) := true

// utils
lazy val pUtils = utils()

lazy val pParser = parser()

lazy val pTypes = types()
  .dependsOn(pUtils)

lazy val pTypesJson = typesJson()
  .dependsOn(pTypes)

lazy val pAst = ast()
  .dependsOn(pTypes)
  .dependsOn(pContractLib)

lazy val pInterperter = interpreter()
  .dependsOn(pAst)
  .dependsOn(pTypesJson)
  .dependsOn(pTrie)
  .dependsOn(pSandBox)
  .dependsOn(pParser)

lazy val pJsonRpc = jsonrpc()

lazy val pTrie = trie()
  .dependsOn(pUtils)

lazy val pContractLib = contractLib()
  .dependsOn(pTypes)

lazy val pSandBox = sandBox().dependsOn(pTypes)

lazy val pTool = tool()
  .dependsOn(pInterperter)
  .dependsOn(pJsonRpc)
  .dependsOn(pSandBox)
  .settings(
    mainClass in assembly := Some("fssi.tool.ToolMain"),
    assemblyMergeStrategy in assembly := {
      case "module-info.class"          => MergeStrategy.discard
      case PathList("META-INF", xs @ _) => MergeStrategy.discard
      case "config-sample.conf"         => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(prependShellScript = Some(defaultShellScript)),
    assemblyJarName in assembly := s"${name.value}",
    test in assembly := {}
  )

lazy val pCoreNode = coreNode()
  .dependsOn(pInterperter)
  .settings(
    mainClass in assembly := Some("fssi.corenode.CoreNodeMain"),
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

lazy val pEdgeNode = edgeNode()
  .dependsOn(pInterperter)
  .dependsOn(pJsonRpc)
  .settings(
    mainClass in assembly := Some("fssi.edgenode.EdgeNodeMain"),
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

addCommandAlias(
  "assemblyAll",
  ";project tool;clean;assembly;project coreNode;clean;assembly;project edgeNode;clean;assembly")
