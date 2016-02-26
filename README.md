# Parinfer on the JVM [![Build Status](https://travis-ci.org/oakmac/parinfer-jvm.svg?branch=master)](https://travis-ci.org/oakmac/parinfer-jvm)

A [Parinfer] implementation written for the JVM in [Kotlin].

## About

There are several popular text editors that run on the JVM. Having a fast
Parinfer implementation available on the JVM allows Parinfer to reach more
editors.

This is basically a 1-to-1 port of [parinfer.js].

The `.json` files in the [tests] folder are copied directly from the [main
Parinfer repo].

## Development Setup

```sh
# run a gradle build and install the artifact in your local repo
# NOTE: this step may take a while
./gradlew install
```

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
[ISC License]:LICENSE.md
