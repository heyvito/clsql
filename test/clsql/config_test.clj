(ns clsql.config-test
  (:require [clojure.test :refer :all]
            [clsql.config :refer :all]
            [clsql.helpers :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(import-private-functions clsql.config [keywordize
                                        normalize-database-uri
                                        find-env-keys
                                        get-env])

(deftest test-keywordize
  (is (= :this-is-a-test (keywordize "this_is_a_test"))))

(deftest test-normalize-database-uri
  (testing "without prefix"
    (let [input {:connection-uri "postgresql://127.0.0.1:5432/foo"}
          output (normalize-database-uri input)]
      (is (contains? output :connection-uri))
      (is (= "jdbc:postgresql://127.0.0.1:5432/foo" (:connection-uri output)))))
  (testing "with prefix"
    (let [input {:connection-uri "jdbc:postgresql://127.0.0.1:5432/foo"}
          output (normalize-database-uri input)]
      (is (contains? output :connection-uri))
      (is (= "jdbc:postgresql://127.0.0.1:5432/foo" (:connection-uri output))))))

(deftest test-get-env
  (is (= (get-env) (System/getenv))))

(defmacro with-envs [data & forms]
  `(with-redefs-fn {#'clsql.config/get-env (constantly ~data)}
     #(do ~@forms)))

(defn reset-config!
  ([] (reset-config! nil))
  ([v] (reset! database-configuration v)))

(deftest test-find-env-keys
  (testing "with keys"
    (with-envs {"CLSQL_FOO" "HELLO"}
      (let [keys (find-env-keys)]
        (is (seq keys))
        (is (= "HELLO" (:foo keys))))))
  (testing "without keys"
    (is (empty? (find-env-keys)))))

(deftest test-detect-database-config
  (testing "with loaded data"
    (reset-config! {:foo "bar"})
    (is (= {:foo "bar"} (detect-database-config))))

  (testing "without loaded data and no env"
    (reset-config!)
    (is (nil? (detect-database-config))))

  (testing "without loaded data and env"
    (reset-config!)
    (with-envs {"CLSQL_FOO" "HELLO"}
      (is (= {:foo "HELLO"} (detect-database-config))))))

(deftest test-detect-database-config!
  (testing "with loaded data"
    (reset-config! {:foo "bar"})
    (is (= {:foo "bar"} (detect-database-config!))))

  (testing "without loaded data and no env"
    (reset-config!)
    (is (thrown? ExceptionInfo (detect-database-config!))))

  (testing "without loaded data and env"
    (reset-config!)
    (with-envs {"CLSQL_FOO" "HELLO"}
      (is (= {:foo "HELLO"} (detect-database-config!))))))
