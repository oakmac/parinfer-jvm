# Parinfer on the JVM

A [Parinfer] implementation written for the JVM in [Kotlin].

## About

There are several popular text editors that run on the JVM. Having a fast
Parinfer implementation available on the JVM allows Parinfer to reach more
editors.

This is basically a 1-to-1 copy of [parinfer.js].

The `.json` files in the [tests] folder are copied directly from the [main
Parinfer repo].

I am a total novice to the JVM ecosystem and the Kotlin language. There is
likely lots of room for improvement here. PR's welcome :)

## Development Setup

Install [Leiningen].

```sh
# run a gradle build
cd parinfer
./gradlew install
```

```sh
# run the tests
cd parinfer-test
lein test
```

```sh
# run the benchmarks
cd parinfer-test
lein with-profile bench run
```


## Development Wishlist

* [x] Get something compiling using Kotlin
* [x] Port [parinfer.js] functions
* [x] Set up a test harness using Clojure
* [x] Get all the tests passing
* [x] Wrap tests using a testing library
* [ ] Write a performance test
* [ ] Publish to the Java-equivalent of [npm]
* [ ] Hand off ownership of this project
* [ ] Never use a statically-typed language again ;)

## License

[ISC License]

[Parinfer]:http://shaunlebron.github.io/parinfer/
[Kotlin]:https://kotlinlang.org/
[parinfer.js]:https://github.com/shaunlebron/parinfer/blob/master/lib/parinfer.js
[tests]:parinfer-test/tests/
[main Parinfer repo]:https://github.com/shaunlebron/parinfer/tree/master/lib/test/cases
[Leiningen]:http://leiningen.org/
[npm]:https://www.npmjs.com/
[ISC License]:LICENSE.md
