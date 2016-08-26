/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }
import java.util.concurrent.{ Executor, Executors }
import scala.reflect.ClassTag
import scala.annotation.tailrec

/**
 * Data structure for describing an actor’s deployment details like which
 * executor to run it on.
 *
 * Deliberately not sealed in order to emphasize future extensibility by the
 * framework—this is not intended to be extended by user code.
 */
abstract class DeploymentConfig {
  def next: DeploymentConfig

  def withDispatcherDefault: DeploymentConfig = DispatcherDefault(this)
  def withDispatcherFromConfig(path: String): DeploymentConfig = DispatcherFromConfig(path, this)
  def withDispatcherFromExecutor(executor: Executor): DeploymentConfig = DispatcherFromExecutor(executor, this)
  def withDispatcherFromExecutionContext(ec: ExecutionContext): DeploymentConfig = DispatcherFromExecutionContext(ec, this)

  def withMailboxCapacity(capacity: Int): DeploymentConfig = MailboxCapacity(capacity, this)
}

object DeploymentConfig {
  def apply[T: ClassTag](deployment: DeploymentConfig, default: T): T = {
    @tailrec def rec(d: DeploymentConfig): T = {
      d match {
        case EmptyDeploymentConfig ⇒ default
        case t: T                  ⇒ t
        case _                     ⇒ rec(d.next)
      }
    }
    rec(deployment)
  }
}

case class MailboxCapacity(capacity: Int, next: DeploymentConfig = EmptyDeploymentConfig) extends DeploymentConfig

case object EmptyDeploymentConfig extends DeploymentConfig {
  def next = throw new NoSuchElementException("EmptyDeploymentConfig has no next")
}

sealed trait DispatcherSelector extends DeploymentConfig

sealed case class DispatcherDefault(next: DeploymentConfig) extends DispatcherSelector
object DispatcherDefault extends DispatcherDefault(EmptyDeploymentConfig)
final case class DispatcherFromConfig(path: String, next: DeploymentConfig = EmptyDeploymentConfig) extends DispatcherSelector
final case class DispatcherFromExecutor(executor: Executor, next: DeploymentConfig = EmptyDeploymentConfig) extends DispatcherSelector
final case class DispatcherFromExecutionContext(ec: ExecutionContext, next: DeploymentConfig = EmptyDeploymentConfig) extends DispatcherSelector

trait Dispatchers {
  def lookup(selector: DispatcherSelector): ExecutionContextExecutor
  def shutdown(): Unit
}
