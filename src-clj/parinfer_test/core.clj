(ns parinfer-test.core
  (:import [com.github.oakmac.parinfer ParinferKt])
  (:require
    [clojure.data.json :as json]
    [clojure.string :refer [join]]
    [clojure.walk :refer [keywordize-keys]]))

(def indent-mode-tests (-> "tests/indent-mode.json" slurp json/read-str keywordize-keys))
(def paren-mode-tests (-> "tests/paren-mode.json" slurp json/read-str keywordize-keys))

(defn- result->map
  "Convert a ParinferResult to a Clojure Map."
  [result-obj]
  {:text (.-text result-obj)
   :success (.-success result-obj)
   :error (.-error result-obj)
   :changed-lines (.-changedLines result-obj)})

(defn- run-indent-mode-tests!
  []
  (doseq [test indent-mode-tests]
  ;(let [test (first indent-mode-tests)]
    (let [test-id (get-in test [:in :fileLineNo])
          in-text (join "\n" (get-in test [:in :lines]))
          expected-text (join "\n" (get-in test [:out :lines]))

          cursor-x (get-in test [:in :cursor :cursorX] nil)
          cursor-x (if (nil? cursor-x) nil (int cursor-x))

          cursor-line (get-in test [:in :cursor :cursorLine] nil)
          cursor-line (if (nil? cursor-line) nil (int cursor-line))

          cursor-dx (get-in test [:in :cursor :cursorDx] nil)
          cursor-dx (if (nil? cursor-dx) nil (int cursor-dx))

          result-obj (ParinferKt/indentMode in-text cursor-x cursor-line cursor-dx)
          result (result->map result-obj)]
      ; (println (str "xx" in-text "xx"))
      ; (println (str "zz" expected-text "zz"))
      ; (println (str "yy" (:text result) "yy"))
      ; (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"))))
      (if (not= expected-text (:text result))
        (println (str "Test " test-id " FAILED!!!"))
        (println (str "Test " test-id " passed"))))))

(defn -main [& args]
  (run-indent-mode-tests!))
  ;(run-paren-mode-tests!))

  ; (-> (ParinferKt/indentMode "(def foo\n[a b\nc])" nil)
  ;     result->map
  ;     pr-str
  ;     print))
