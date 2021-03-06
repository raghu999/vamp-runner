package io.vamp.runner

import akka.actor.ActorLogging
import org.json4s.native.Serialization._

import scala.io.Source
import scala.language.postfixOps
import scala.reflect.io.File
import scala.util.Try

trait RecipeLoader {
  this: ActorLogging ⇒

  private implicit val formats = Json.format

  private val files = Config.stringList("vamp.runner.recipes.files")

  protected def load: List[Recipe] = {
    files.flatMap { file ⇒

      log.info(s"Loading recipe: $file")

      def load(relative: String): String = {
        Source.fromFile(s"${File(file).parent.toString}${File.separator}$relative").mkString
      }

      val recipe = Try {
        Option {
          read[Recipe](Source.fromFile(file).mkString)
        }
      } recover {
        case e: Throwable ⇒ log.error(e, e.getMessage); None
      } get

      recipe map { recipe ⇒

        log.info(s"Recipe : ${recipe.name}")
        recipe.run.foreach { run ⇒ log.info(s"Run    : ${run.description}") }
        recipe.cleanup.foreach { cleanup ⇒ log.info(s"Cleanup: ${cleanup.description}") }

        recipe.copy(
          run = recipe.run.map { run ⇒ run.copy(resource = load(run.resource)) },
          cleanup = recipe.cleanup.map { cleanup ⇒ cleanup.copy(resource = load(cleanup.resource)) }
        )
      }
    }
  }
}
