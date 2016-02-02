# Parinfer on the JVM

A [Parinfer] implementation written for the JVM.

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

Instal [Kotlin] and [Leiningen].

```sh
# compile a parinfer.jar file
kotlinc src/com/oakmac/parinfer/parinfer.kt -include-runtime -d lib/parinfer.jar

# run the tests
lein run
```

## Development Wishlist

* [x] Get something compiling using Kotlin
* [x] Port [parinfer.js] functions
* [x] Set up a test harness using Clojure
* [x] Get all the tests passing
* [x] Wrap tests using an idiomatic testing library
* [ ] Publish to the Java-equivalent of [npm]
* [ ] Write a performance test
* [ ] Hand off ownership of this project
* [ ] Never use a statically-typed language again ;)

## License

[ISC License]

[Parinfer]:http://shaunlebron.github.io/parinfer/
[parinfer.js]:https://github.com/shaunlebron/parinfer/blob/master/lib/parinfer.js
[tests]:tests/
[main Parinfer repo]:https://github.com/shaunlebron/parinfer/tree/master/lib/test/cases
[npm]:https://www.npmjs.com/
[Kotlin]:https://kotlinlang.org/docs/tutorials/command-line.html
[Leiningen]:http://leiningen.org/
[ISC License]:LICENSE.md
