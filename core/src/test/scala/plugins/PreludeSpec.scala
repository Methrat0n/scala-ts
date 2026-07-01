package io.github.scalats.plugins

import java.io.{ File, PrintStream }

import scala.io.Source.fromFile

import io.github.scalats.ast.Declaration
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.{ Logger, Settings }
import io.github.scalats.tsconfig.ConfigFactory

final class PreludeSpec extends org.specs2.mutable.Specification {
  "Printer prelude".title

  sequential

  def withTempDir[T](f: File => T): T = {
    val tmp = File.createTempFile("PreludeSpec", "")
    tmp.delete()
    tmp.mkdirs()

    try {
      f(tmp)
    } finally {
      def deleteRec(d: File): Unit = {
        Option(d.listFiles).foreach(_.foreach(deleteRec))
        val _ = d.delete()
      }

      deleteRec(tmp)
    }
  }

  "File printer" should {
    "keep per-instance prelude content" in withTempDir { outDir =>
      val tsPrinter = new FilePrinter(outDir)
      tsPrinter.configurePrelude("// TypeScript prelude")

      val conf = Settings()

      withPrinter(
        tsPrinter(
          conf,
          Declaration.Interface,
          ListSet(),
          "Foo",
          ListSet.empty
        )
      ) { out =>
        out.println("export type Foo = string")
        out.flush()
      }

      fromFile(new File(outDir, "Foo.ts")).mkString must contain(
        "// TypeScript prelude"
      )
    }

    "not read a concurrently mutated prelude file when configured" in
      withTempDir { outDir =>
        val preludeFile = File.createTempFile("PreludeSpec", ".prelude")
        preludeFile.deleteOnExit()

        def writePrelude(comment: String): Unit = {
          val out = new PrintStream(preludeFile)
          out.println(comment)
          out.close()
        }

        writePrelude("// TypeScript prelude")

        val printer = new FilePrinter(outDir)
        printer.configurePrelude("// TypeScript prelude")

        writePrelude("# Python prelude")

        val conf = Settings()

        withPrinter(
          printer(
            conf,
            Declaration.Interface,
            ListSet(),
            "Bar",
            ListSet.empty
          )
        ) { out =>
          out.println("export type Bar = string")
          out.flush()
        }

        val content = fromFile(new File(outDir, "Bar.ts")).mkString

        content must contain("// TypeScript prelude")
        content must not contain ("# Python prelude")
      }
  }

  "Configuration" should {
    "configure the printer prelude from plugin configuration" in withTempDir {
      outDir =>
        val source = ConfigFactory.parseString(s"""
printer = "io.github.scalats.plugins.FilePrinter"
printerPrelude = [ "// From configuration" ]
""")

        val cfg = Configuration.load(
          source,
          Logger(org.slf4j.LoggerFactory getLogger getClass),
          Some(outDir)
        )

        cfg.printerPrelude must beSome("// From configuration")

        val conf = Settings()

        withPrinter(
          cfg.printer(
            conf,
            Declaration.Interface,
            ListSet(),
            "Baz",
            ListSet.empty
          )
        ) { out =>
          out.println("export type Baz = string")
          out.flush()
        }

        fromFile(new File(outDir, "Baz.ts")).mkString must contain(
          "// From configuration"
        )
    }
  }

  private def withPrinter[T](p: => PrintStream)(f: PrintStream => T): T = {
    var w: PrintStream = null

    try {
      w = p
      f(w)
    } finally {
      if (w != null) {
        w.close()
      }
    }
  }
}
