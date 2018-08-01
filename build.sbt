import Common._, prj._


coverageExcludedFiles in ThisBuild := ".*macro.*"
coverageEnabled.in(Test, test) := true
parallelExecution in ThisBuild := false
fork in ThisBuild := true
ensimeScalaVersion in ThisBuild := "2.12.4"
ensimeIgnoreMissingDirectories in ThisBuild := true

