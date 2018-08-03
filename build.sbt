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
