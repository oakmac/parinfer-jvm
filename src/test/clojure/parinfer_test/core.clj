(ns parinfer-test.core
  (:require
    [clojure.data.json :as json]
    [clojure.string :refer [join]]
    [clojure.test :refer [deftest is run-tests]]
    [clojure.walk :refer [keywordize-keys]])
  (:import (com.oakmac.parinfer Parinfer ParinferResult)))

;; load test cases
(def indent-mode-cases (-> "tests/indent-mode.json" slurp json/read-str keywordize-keys))
(def paren-mode-cases (-> "tests/paren-mode.json" slurp json/read-str keywordize-keys))

(defn- indent-mode
  "Wrapper around ParinferKt/indentMode"
  [txt cursor-x cursor-line cursor-dx preview]
  (Parinfer/indentMode txt
                       (when cursor-x (int cursor-x))
                       (when cursor-line (int cursor-line))
                       (when cursor-dx (int cursor-dx))
                       preview))

(defn- paren-mode
  "Wrapper around ParinferKt/parenMode"
  [txt cursor-x cursor-line cursor-dx preview]
  (Parinfer/parenMode txt
                      (when cursor-x (int cursor-x))
                      (when cursor-line (int cursor-line))
                      (when cursor-dx (int cursor-dx))
                      preview))

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
    (let [test-id (get-in test [:source :lineNo])
          in-text (get-in test [:text])
          expected-text (get-in test [:result :text])

          cursor-x (get-in test [:options :cursorX])
          cursor-line (get-in test [:options :cursorLine])

          ;; TODO: cursorDx and previewCursorScope appear to be deprecated
          cursor-dx (get-in test [:result :cursorDx])
          preview-cursor-scope (or (get-in test [:result :previewCursorScope]) false)

          error {:name (get-in test [:result :error :name])
                 :line (get-in test [:result :error :lineNo])
                 :x    (get-in test [:result :error :x])}

          result ^ParinferResult (if (= mode :indent)
                                   (indent-mode in-text cursor-x cursor-line cursor-dx preview-cursor-scope)
                                   (paren-mode in-text cursor-x cursor-line cursor-dx preview-cursor-scope))

          result2 (if (= mode :indent)
                    (indent-mode (result-text result) cursor-x cursor-line cursor-dx preview-cursor-scope)
                    (paren-mode (result-text result) cursor-x cursor-line cursor-dx preview-cursor-scope))]
      (is (= (result-text result) expected-text) (str "in/out text: test id " test-id))
      (when-let [x (get-in test [:result :cursorX])]
        (is (= x (result-x result)) (str "cursorX: test id " test-id)))
      (if-let [e (result-error result)]
        (is (= e error) (str "error: test id " test-id))
        (do
          (when-not cursor-dx
            (is (= (result-text result2) expected-text) (str "idempotence: test id " test-id)))
          (when (= nil cursor-x cursor-line cursor-dx)
            (let [result3 (if (= mode :indent)
                              (paren-mode (result-text result) nil nil nil preview-cursor-scope)
                              (indent-mode (result-text result) nil nil nil preview-cursor-scope))]
              (is (= (result-text result3) expected-text) (str "cross-mode preservation: test id " test-id)))))))))

(deftest indent-mode-test
  (check-cases :indent indent-mode-cases))

(deftest paren-mode-test
  (check-cases :paren paren-mode-cases))
