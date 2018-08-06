import Common._, prj._

coverageExcludedFiles in ThisBuild := ".*macro.*"
coverageEnabled.in(Test, test) := true
parallelExecution in ThisBuild := false
fork in ThisBuild := true

// utils
lazy val pUtils = utils()

lazy val pTypes = types()
  .dependsOn(pUtils)

lazy val pTypesJson = typesJson()
  .dependsOn(pTypes)

lazy val pAst = ast()
  .dependsOn(pTypes)

lazy val pInterperter = interpreter()
  .dependsOn(pAst)
  .dependsOn(pTypesJson)

lazy val pTool = tool()
  .dependsOn(pInterperter)
  .settings(
    mainClass in assembly := Some("fssi.tool.ToolMain"),
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
