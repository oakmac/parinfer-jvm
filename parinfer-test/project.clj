(defproject parinfer-test "0.1.0"

  :description "The test harness for parinfer-jvm"
  :url "https://github.com/oakmac/parinfer-jvm"

  :license {:name "ISC License"
            :url "https://github.com/oakmac/parinfer-jvm/blob/master/LICENSE.md"
            :distribution "repo"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.oakmac/parinfer "1.0-SNAPSHOT"]
                 [criterium "0.4.3"]]

  :main paredit.performance

  :profiles {:bench {:jvm-opts ["-server"]}})
