(ns clsql.grammars.migration-test
  (:require [clojure.test :refer :all]
            [clsql.grammars.migration :refer :all]
            [clsql.helpers :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-parser
  (let [info (parse-migration "test/fixtures/migration-test.sql")]
    (is (contains? info :up))
    (is (contains? info :down))
    (is (= (:up info)
           (str "CREATE TABLE users\n"
                "    (id INTEGER NOT NULL PRIMARY KEY,\n"
                "     name VARCHAR NOT NULL, -- We're allowing comments to be here as well.\n"
                "     email TEXT NOT NULL UNIQUE);")))
    (is (= (:down info) "DROP TABLE users;"))))

(import-private-functions clsql.grammars.migration [extract-from-ast])

(deftest test-extract-from-ast
  (testing "valid key"
    (let [ast [:test 1]]
      (is (= 1 (extract-from-ast :test ast)))))
  (testing "invalid key"
    (let [ast [:test 1]]
      (is (thrown? ExceptionInfo (extract-from-ast :foo ast))))))
