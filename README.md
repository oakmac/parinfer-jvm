# Parinfer on the JVM [![Build Status](https://travis-ci.org/oakmac/parinfer-jvm.svg?branch=master)](https://travis-ci.org/oakmac/parinfer-jvm)

A [Parinfer] implementation written for the JVM in [Kotlin].

## About

There are several popular text editors that run on the JVM. Having a fast
Parinfer implementation available on the JVM allows Parinfer to reach more
editors.

This implementation is a 1-to-1 port of [parinfer.js].
It strives to be feature equivalent with parinfer as described in the
[Indent Mode](http://shaunlebron.github.io/parinfer/#indent-mode)
and [Paren Mode](http://shaunlebron.github.io/parinfer/#paren-mode)
sections of the main website.

If you find behavior that does not match [parinfer.js], please report it as an issue in this project.

The `.json` files in the [tests] folder are copied directly from the [main
Parinfer repo].

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.oakes/parinfer.svg)](https://clojars.org/org.clojars.oakes/parinfer)

or build/install from source (see Development Setup below).

## Usage

```clojure
(ns ...
  (:import [com.oakmac.parinfer Parinfer]))

(Parinfer/parenMode text x line nil false)
(Parinfer/indentMode text x line nil preview-cursor-scope?)
```

Alternatively see [cross-parinfer] which wraps this project and provides a Clojure/ClojureScript interface.

## Development Setup

```sh
# run a gradle build and install the artifact in your local repo
# NOTE: this step may take a while
./gradlew install
```

The resulting jar file will be under `build/lib`

```sh
# run the tests
./gradlew clean clojureTest
```

To run the performance benchmarks, comment out the following block in
`build.gradle`:

```groovy
sourceSets.test.clojure {
  excludeNamespace 'parinfer-test.performance'
}
```

Then run the tests as above. There is probably a better way to do this, but it's
way beyond my Gradle-fu. PR welcome!

## License

[ISC License]

[Parinfer]:http://shaunlebron.github.io/parinfer/
[Kotlin]:https://kotlinlang.org/
[parinfer.js]:https://github.com/shaunlebron/parinfer/blob/master/lib/parinfer.js
[tests]:tests/
[main Parinfer repo]:https://github.com/shaunlebron/parinfer/tree/master/lib/test/cases
[cross-parinfer]:https://github.com/oakes/cross-parinfer
[ISC License]:LICENSE.md
