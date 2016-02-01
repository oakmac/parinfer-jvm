# Parinfer on the JVM [**WORK IN PROGRESS**]

## Current Development Status

* [x] Get something compiling using Kotlin
* [x] Port [parinfer.js] functions
* [x] Set up a test harness using Clojure
* [ ] Get all the tests passing
* [ ] Hand off ownership of this project
* [ ] Never use a statically-typed language again

## Development Setup

Instal [Kotlin] and [Leiningen].

```sh
# compile a parinfer.jar file
kotlinc src-kt/parinfer.kt -include-runtime -d lib/parinfer.jar

# run the tests
lein run
```

## License

[ISC License]

[parinfer.js]:https://github.com/shaunlebron/parinfer/blob/master/lib/parinfer.js
[Kotlin]:https://kotlinlang.org/docs/tutorials/command-line.html
[Leiningen]:http://leiningen.org/
[ISC License]:LICENSE.md
