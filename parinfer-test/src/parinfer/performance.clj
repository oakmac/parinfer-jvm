(ns parinfer.performance
  (:require [criterium.core :as perf])
  (:import (com.oakmac.parinfer ParinferKt)))

(defn benchmark-large-file []
  (let [lots-o-clojure (slurp "tests/really_long_file")]
    (perf/bench (ParinferKt/indentMode lots-o-clojure nil nil nil))
    (perf/bench (ParinferKt/parenMode lots-o-clojure nil nil nil))))

(defn -main [& args]
  (benchmark-large-file))
