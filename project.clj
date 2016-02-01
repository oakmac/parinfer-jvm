(defproject parinfer-test "0.1.0"
  :description "The test harness for parinfer-jvm"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]]
  :resource-paths ["lib/parinfer.jar"]
  :source-paths ["src-clj"]
  :main parinfer-test.core)

;; TODO: add url, license
