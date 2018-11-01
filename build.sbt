import Common._, prj._

coverageExcludedFiles in ThisBuild := ".*macro.*"
parallelExecution in ThisBuild := false
fork in ThisBuild := true
scalaVersion in ThisBuild := "2.12.4"
coverageEnabled in (Test, test) := true
enablePlugins(PackPlugin)

lazy val pBase = base()

// utils
lazy val pUtils = utils().dependsOn(pBase)

lazy val pTypes = types()
  .dependsOn(pUtils)

lazy val pTypesJson = typesJson()
  .dependsOn(pTypes)

lazy val pAst = ast()
  .dependsOn(pTypes)
  //.dependsOn(pContractLib)

lazy val pScp = scp()
  .dependsOn(pUtils)

lazy val pInterperter = interpreter()
  .dependsOn(pAst)
  .dependsOn(pTypesJson)
  .dependsOn(pTrie)
  .dependsOn(pSandBox)
  .dependsOn(pContractScaffold)
  .dependsOn(pScp)

lazy val pJsonRpc = jsonrpc()

lazy val pTrie = trie()
  .dependsOn(pUtils)

lazy val pStore = store()

lazy val pContractLib = contractLib()
  .dependsOn(pTypes)

lazy val pSandBox = sandBox().dependsOn(pTypes).dependsOn(pContractLib)

lazy val pContractScaffold = contractScaffold().dependsOn(pTypes)

lazy val pTool = tool()
  .dependsOn(pInterperter)
  .dependsOn(pJsonRpc)
  .dependsOn(pSandBox)
  .settings(
    packMain := Map("tool" -> "fssi.tool.ToolMain")
  )

lazy val pCoreNode = coreNode()
  .dependsOn(pInterperter)
  .settings(
    packMain := Map("corenode" -> "fssi.corenode.CoreNodeMain")
  )

lazy val pEdgeNode = edgeNode()
  .dependsOn(pInterperter)
  .dependsOn(pJsonRpc)
  .settings(
    packMain := Map("edgenode" -> "fssi.edgenode.EdgeNodeMain")
  )

addCommandAlias(
  "packInstallAll",
  ";project tool;clean;packInstall;project coreNode;clean;packInstall;project edgeNode;clean;packInstall")
addCommandAlias("packInstallTool", ";project /;clean;project tool;packInstall")
addCommandAlias("packInstallCoreNode", ";project /;clean;project coreNode;packInstall")
addCommandAlias("packInstallEdgeNode", ";project /;clean;project edgeNode;packInstall")
