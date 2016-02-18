(ns parinfer-test.core
  (:require
    [clojure.data.json :as json]
    [clojure.string :refer [join]]
    [clojure.test :refer [deftest is run-tests]]
    [clojure.walk :refer [keywordize-keys]])
  (:import (com.oakmac.parinfer Parinfer)))

;; load test cases
(def indent-mode-cases (-> "tests/indent-mode.json" slurp json/read-str keywordize-keys))
(def paren-mode-cases  (-> "tests/paren-mode.json"  slurp json/read-str keywordize-keys))

(defn- indent-mode
  "Wrapper around ParinferKt/indentMode
   Returns only the result text"
  [txt cursor-x cursor-line cursor-dx]
  (.-text (Parinfer/indentMode txt cursor-x cursor-line cursor-dx)))

(defn- paren-mode
  "Wrapper around ParinferKt/parenMode
   Returns only the result text"
  [txt cursor-x cursor-line cursor-dx]
  (.-text (Parinfer/parenMode txt cursor-x cursor-line cursor-dx)))

(defn- check-cases
  [mode test-cases]
  (doseq [test test-cases]
    (let [test-id (get-in test [:in :fileLineNo])
          in-text (join "\n" (get-in test [:in :lines]))
          expected-text (join "\n" (get-in test [:out :lines]))

          cursor-x (get-in test [:in :cursor :cursorX] nil)
          cursor-x (if (nil? cursor-x) nil (int cursor-x))

          cursor-line (get-in test [:in :cursor :cursorLine] nil)
          cursor-line (if (nil? cursor-line) nil (int cursor-line))

          cursor-dx (get-in test [:in :cursor :cursorDx] nil)
          cursor-dx (if (nil? cursor-dx) nil (int cursor-dx))

          out-text (if (= mode :indent)
                     (indent-mode in-text cursor-x cursor-line cursor-dx)
                     (paren-mode in-text cursor-x cursor-line cursor-dx))

          out-text2 (if (= mode :indent)
                      (indent-mode out-text cursor-x cursor-line cursor-dx)
                      (paren-mode out-text cursor-x cursor-line cursor-dx))]
      (is (= out-text expected-text) (str "in/out text: test id " test-id))
      (is (= out-text2 expected-text) (str "idempotence: test id " test-id))
      (when (= nil cursor-x cursor-line cursor-dx)
        (let [out-text3 (if (= mode :indent)
                          (paren-mode out-text nil nil nil)
                          (indent-mode out-text nil nil nil))]
          (is (= out-text3 expected-text) (str "cross-mode preservation: test id " test-id)))))))

(deftest indent-mode-test
  (check-cases :indent indent-mode-cases))

(deftest paren-mode-test
  (check-cases :paren paren-mode-cases))
