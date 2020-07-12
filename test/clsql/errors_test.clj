(ns clsql.errors-test
  (:require [clojure.test :refer :all]
            [clsql.errors :refer [throw-if]]
            [clsql.grammars.queries :refer :all]
            [clsql.helpers :refer [import-private-functions]]
            [instaparse.core :as insta]))

(import-private-functions clsql.errors [format-failure])

(deftest test-throw-if
  (testing "with a false predicate"
    (is (nil? (throw-if false "Boom!"))))
  (testing "with a true predicate"
    (is (thrown? IllegalArgumentException (throw-if true "Boom!")))))

(deftest test-parser-errors
  (let [data "This is clearly wrong."
        failure (parser data)]
    (is (insta/failure? failure))
    (is (= (format-failure "test.sql" failure)
           (str "Error parsing test.sql at line 1 column 1:\n"
           "This is clearly wrong.\n"
           "^\n"
                "Expected one of:\n"
                "\"--\"\n"
                "\"\\r\\n\"\n"
                "\"\\n\"\n"
                "\"\\t\"\n"
                "\" \"")))))
