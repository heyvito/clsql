(ns clsql.grammars.queries-test
  (:require [clojure.test :refer :all]
            [clsql.grammars.queries :refer [parse-queries]]))

(deftest test-parser
  (let [info (parse-queries "test/fixtures/queries/full-query.sql")]
    (is (= 1 (count info)))
    (let [query (first info)]
      (is (contains? query :header))
      (is (contains? query :body))
      (let [{:keys [header body]} query]
        (is (contains? header :name))
        (is (contains? header :modifiers))
        (is (contains? header :docstring))
        (is (contains? header :placeholders))
        (is (vector? body))
        (is (every? vector? body))
        (is (every? #(= 2 (count %)) body))))))

(deftest test-parser-complex
  (let [info (parse-queries "test/fixtures/queries/complex-query.sql")]
    (is (= 1 (count info)))
    (let [query (first info)]
      (is (contains? query :header))
      (is (contains? query :body))
      (let [{:keys [header body]} query]
        (is (contains? header :name))
        (is (contains? header :docstring))
        (is (contains? header :placeholders))
        (is (vector? body))
        (is (every? vector? body))
        (is (every? #(= 2 (count %)) body))))))
