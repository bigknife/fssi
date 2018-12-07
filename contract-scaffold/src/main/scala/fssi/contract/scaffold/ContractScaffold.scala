package fssi
package contract
package scaffold

import java.nio.file._
import fssi.types.exception._
import directory._
import file._

class ContractScaffold {

  private lazy val directory = new ContractDirectory

  private lazy val file = new ContractFile

  def createContractProject(projectRoot: Path): Either[FSSIException, Unit] = {
    for {
      _ <- directory.createDefaultDirectory(projectRoot)
      _ <- file.createDefaultFiles(projectRoot)
    } yield ()
  }
}
