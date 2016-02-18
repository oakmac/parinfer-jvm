(ns parinfer.performance
  (:require [criterium.core :as perf])
  (:import (com.oakmac.parinfer Parinfer)))

(defn benchmark-large-file []
  (let [lots-o-clojure (slurp "tests/really_long_file")]
    (perf/bench (Parinfer/indentMode lots-o-clojure nil nil nil))
    (perf/bench (Parinfer/parenMode lots-o-clojure nil nil nil))))

(defn -main [& args]
  (benchmark-large-file))
