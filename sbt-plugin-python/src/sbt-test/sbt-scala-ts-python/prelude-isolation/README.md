Reproduces the original prelude race: TypeScript and Python backends compiled in
parallel in the same sbt JVM (`sbt compile` on the aggregate root).

Before the fix, TypeScript files could start with a Python `#` prelude about
half of the time because both backends shared:

- the JVM-global `scala-ts.printer.prelude-url` system property, and
- a mutable `target/scala-ts-prelude.tmp` file.

Run from the repository root (after `publishLocal`):

    sbt "sbt-plugin-python/scripted sbt-scala-ts-python/prelude-isolation"

To observe the flaky failure, revert the prelude isolation fix and re-run; the
grep checks on `Foo.ts` will intermittently fail.

See also `core/src/test/scala/plugins/PreludeRaceReproductionSpec.scala` for a
fast, deterministic unit-level reproduction of the legacy mechanism.
