(ns parinfer-test.core
  (:require
    [clojure.data.json :as json]
    [clojure.string :refer [join]]
    [clojure.test :refer [deftest is run-tests]]
    [clojure.walk :refer [keywordize-keys]])
  (:import (com.oakmac.parinfer Parinfer ParinferResult ParinferException)))

;; load test cases
(def indent-mode-cases (-> "tests/indent-mode.json" slurp json/read-str keywordize-keys))
(def paren-mode-cases (-> "tests/paren-mode.json" slurp json/read-str keywordize-keys))

(defn- indent-mode
  "Wrapper around ParinferKt/indentMode"
  [txt cursor-x cursor-line cursor-dx]
  (Parinfer/indentMode txt
                       (when cursor-x (int cursor-x))
                       (when cursor-line (int cursor-line))
                       (when cursor-dx (int cursor-dx))))

(defn- paren-mode
  "Wrapper around ParinferKt/parenMode"
  [txt cursor-x cursor-line cursor-dx]
  (Parinfer/parenMode txt
                      (when cursor-x (int cursor-x))
                      (when cursor-line (int cursor-line))
                      (when cursor-dx (int cursor-dx))))

(def error-names {com.oakmac.parinfer.Error/QUOTE_DANGER   "quote-danger"
                  com.oakmac.parinfer.Error/EOL_BACKSLASH  "eol-backslash"
                  com.oakmac.parinfer.Error/UNCLOSED_QUOTE "unclosed-quote"
                  com.oakmac.parinfer.Error/UNCLOSED_PAREN "unclosed-paren"})

(defn result-text [^ParinferResult result]
  (.-text result))

(defn result-x [^ParinferResult result]
  (.-cursorX result))

(defn result-error [^ParinferResult result]
  (when-let [error (.-error result)]
    {:name (get error-names (.getError error))
     :line (.getLineNo error)
     :x    (.getX error)}))


(defn- check-cases
  [mode test-cases]
  (doseq [test test-cases]
    (let [test-id (get-in test [:in :fileLineNo])
          in-text (join "\n" (get-in test [:in :lines]))
          expected-text (join "\n" (get-in test [:out :lines]))

          cursor-x (get-in test [:in :cursor :cursorX])
          cursor-line (get-in test [:in :cursor :cursorLine])
          cursor-dx (get-in test [:in :cursor :cursorDx])

          error {:name (get-in test [:out :error :name])
                 :line (get-in test [:out :error :lineNo])
                 :x    (get-in test [:out :error :x])}

          result ^ParinferResult (if (= mode :indent)
                                   (indent-mode in-text cursor-x cursor-line cursor-dx)
                                   (paren-mode in-text cursor-x cursor-line cursor-dx))

          result2 (if (= mode :indent)
                    (indent-mode (result-text result) cursor-x cursor-line cursor-dx)
                    (paren-mode (result-text result) cursor-x cursor-line cursor-dx))]
      (is (= (result-text result) expected-text) (str "in/out text: test id " test-id))
      (when-let [x (get-in test [:out :cursor :cursorX])]
        (is (= x (result-x result)) (str "cursorX: test id " test-id)))
      (if-let [e (result-error result)]
        (is (= e error) (str "error: test id " test-id))
        (do
          (when-not cursor-dx
            (is (= (result-text result2) expected-text) (str "idempotence: test id " test-id)))
          (when (= nil cursor-x cursor-line cursor-dx)
            (let [result3 (if (= mode :indent)
                              (paren-mode (result-text result) nil nil nil)
                              (indent-mode (result-text result) nil nil nil))]
              (is (= (result-text result3) expected-text) (str "cross-mode preservation: test id " test-id)))))))))

(deftest indent-mode-test
  (check-cases :indent indent-mode-cases))

(deftest paren-mode-test
  (check-cases :paren paren-mode-cases))
