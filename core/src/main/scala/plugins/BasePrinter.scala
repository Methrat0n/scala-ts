package io.github.scalats.plugins

import java.io.PrintStream

import io.github.scalats.ast.{ SingletonTypeRef, TypeRef }
import io.github.scalats.core.{ Printer, Settings }
import io.github.scalats.core.Internals.ListSet

abstract class BasePrinter extends Printer {
  private var configuredPrelude: Option[String] = None

  /**
   * Sets the prelude content for this printer instance.
   *
   * When set, it takes precedence over the legacy
   * `scala-ts.printer.prelude-url` system property.
   *
   * Must be called before the first call to `printPrelude` on this instance
   * (as `Configuration.load` already does).
   */
  private[scalats] def configurePrelude(content: String): Unit =
    configuredPrelude = Some(content)

  private lazy val preludeContent: Option[String] =
    configuredPrelude.orElse {
      sys.props.get("scala-ts.printer.prelude-url").map { url =>
        scala.io.Source.fromURL(url).mkString
      }
    }

  /**
   * If the system property `scala-ts.printer.import-pattern`, it's used to format.
   */
  protected def printPrelude(out: PrintStream): Unit =
    preludeContent.foreach(out.println)

  private lazy val preformatImport: (String, Boolean, String) => String =
    sys.props.get("scala-ts.printer.import-pattern") match {
      case Some(
            pattern
          ) => { (tpeName: String, _: Boolean, importPath: String) =>
        pattern.format(tpeName, importPath)
      }

      case _ => { (tpeName: String, singleton: Boolean, importPath: String) =>
        if (singleton) {
          s"import * as ns${tpeName} from '${importPath}'"
        } else {
          s"import * as ns${tpeName} from '${importPath}';\nimport type { ${tpeName} } from '${importPath}'"
        }
      }
    }

  /**
   * @param importPath the function applied to each required type to determine the import path
   */
  protected def printImports(
      settings: Settings,
      requires: ListSet[TypeRef],
      out: PrintStream
    )(importPath: TypeRef => String
    ): Unit = {
    import settings.{ lineSeparator => sep }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    val requiredImports = requires.map { tpe =>
      // Required can contain different types with same name but not same kind
      // (e.g. class and its companion object), which must be merge as single import there

      val tpeName = typeNaming(tpe)
      val singleton = tpe match {
        case SingletonTypeRef(_, _) => true
        case _                      => false
      }

      val preformatted = preformatImport(tpeName, singleton, importPath(tpe))

      s"${preformatted}${sep}"
    }

    requiredImports.toList.sorted.foreach(out.println)
  }
}
