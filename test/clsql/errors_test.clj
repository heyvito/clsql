(ns clsql.errors-test
  (:require [clojure.test :refer :all]
            [clsql.errors :refer [throw-if]]))

(deftest test-throw-if
  (testing "with a false predicate"
    (is (nil? (throw-if false "Boom!"))))
  (testing "with a true predicate"
    (is (thrown? IllegalArgumentException (throw-if true "Boom!")))))
