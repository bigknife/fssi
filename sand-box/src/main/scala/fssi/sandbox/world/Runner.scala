package fssi
package sandbox
package world
import java.io.File
import java.nio.file.Paths

import fssi.contract.lib.Context
import fssi.sandbox.counter.CostCounter
import fssi.sandbox.exception.ContractRunningException
import fssi.sandbox.loader.ContractClassLoader
import fssi.sandbox.types.SParameterType._
import fssi.sandbox.types.{Method, SParameterType}
import fssi.types.base.UniqueName
import fssi.types.biz.Contract
import fssi.utils.FileUtil
import org.slf4j.{Logger, LoggerFactory}

class Runner {

  private lazy val builder = new Builder

  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  def invokeContractMethod(
      context: Context,
      contractFile: File,
      method: Method,
      parameters: Contract.UserContract.Parameter): Either[ContractRunningException, Unit] = {
    logger.info(
      s"invoke contract method $method with params $parameters for contract $contractFile in context $context")
    if (contractFile.exists() && contractFile.isFile) {
      val rootPath =
        Paths.get(contractFile.getParent, String.valueOf(UniqueName.randomUUID(false).value))
      try {
        val source = Paths.get(rootPath.toString, "source")
        if (!source.toFile.exists()) source.toFile.mkdirs()
        better.files
          .File(contractFile.toPath)
          .unzipTo(source)(java.nio.charset.Charset.forName("utf-8"))
        val target = Paths.get(rootPath.toString, "target")
        if (!target.toFile.exists()) target.toFile.mkdirs()
        for {
          _ <- builder
            .degradeClassVersion(source, target)
            .left
            .map(x => ContractRunningException(x.messages))
        } yield {
          val classLoader          = new ContractClassLoader(target)
          val methodParameterTypes = method.parameterTypes.map(_.`type`)
          val clazz                = classLoader.findClass(method.className)
          val instance             = clazz.newInstance()
          val contractMethod       = clazz.getDeclaredMethod(method.methodName, methodParameterTypes: _*)
          val params               = context +: extractParameterValues(method.parameterTypes, parameters)
          val accessible           = contractMethod.isAccessible
          contractMethod.setAccessible(true)
          contractMethod.invoke(instance, params: _*)
          contractMethod.setAccessible(accessible)
          val throwCost      = CostCounter.throwCost
          val allocationCost = CostCounter.allocationCost
          val jumpCost       = CostCounter.jumpCost
          val methodCallCost = CostCounter.methodCallCost
          logger.info(
            s"invoke contract method success,cost details: \n[\nmethodCallCost: $methodCallCost\nallocationCost: $allocationCost\njumpCost: $jumpCost\nthrowCost: $throwCost\n]")
        }
      } catch {
        case t: Throwable =>
          val error =
            s"invoke contract method $method with params $parameters for contract $contractFile in context $context failed: ${t.getMessage}"
          logger.error(error, t)
          Left(ContractRunningException(Vector(error)))
      } finally { if (rootPath.toFile.exists()) FileUtil.deleteDir(rootPath) }
    } else {
      val ex = ContractRunningException(
        Vector(s"contract $contractFile must be a file assembled all files"))
      logger.error(ex.getMessage, ex)
      Left(ex)
    }
  }

  private def extractParameterValues(parameterTypes: Array[SParameterType],
                                     parameters: Contract.UserContract.Parameter): Array[AnyRef] = {
    logger.info(s"extract params value from $parameters for $parameterTypes")
    import Contract.UserContract.Parameter._
    var index = 0

    def convertToParameterValue(parameter: Contract.UserContract.Parameter,
                                acc: Array[AnyRef]): Array[AnyRef] = {
      index = index + 1
      parameter match {
        case PString(s) => acc :+ s
        case PBool(b)   => acc :+ java.lang.Boolean.valueOf(b)
        case PBigDecimal(bigDecimal) =>
          parameterTypes(index) match {
            case SInt    => acc :+ Integer.valueOf(bigDecimal.intValue())
            case SLong   => acc :+ java.lang.Long.valueOf(bigDecimal.longValue())
            case SFloat  => acc :+ java.lang.Float.valueOf(bigDecimal.floatValue())
            case SDouble => acc :+ java.lang.Double.valueOf(bigDecimal.doubleValue())
            case SString => acc :+ bigDecimal.toPlainString
            case _       => acc
          }
        case PArray(array) =>
          index = index - 1
          array.flatMap(p => convertToParameterValue(p, acc))
      }
    }

    convertToParameterValue(parameters, Array.empty)
  }
}
