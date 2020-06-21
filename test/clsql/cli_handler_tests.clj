(ns clsql.cli-handler-tests
  (:require [clojure.test :refer :all]
            [clsql.helpers :refer [database-config
                                   isolating-database
                                   isolating-config
                                   reset-database!]]
            [clsql.cli-handler :refer [handle-command-line-args]]
            [clsql.config :as config])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-pass-through
  (is (false? (handle-command-line-args)))
  (is (false? (handle-command-line-args ["test"])))
  (is (false? (handle-command-line-args ["test" "with" "more" "params"]))))

(deftest test-create-migration
  (testing "with no params"
    (is (thrown? ExceptionInfo (handle-command-line-args ["create-migration"]))))
  (testing "with invalid name"
    (is (thrown? ExceptionInfo (handle-command-line-args ["create-migration" "invalid name"]))))
  (testing "with correct params"
    (with-redefs-fn {#'clsql.migrator/create-migration (fn [name] (is (= "valid-name-24" name)))}
      #(handle-command-line-args ["create-migration" "valid-name-24"]))))

(deftest test-migrate
  (testing "call is made"
    (let [called (atom false)
          mocked-fn (fn [] (reset! called true))]
      (with-redefs-fn {#'clsql.migrator/migrate mocked-fn}
        #(handle-command-line-args ["db-migrate"]))
      (is @called))))

(deftest test-rollback
  (testing "with invalid args"
    (is (thrown? ExceptionInfo (handle-command-line-args ["db-rollback" "test"]))))
  (testing "with no args"
    (with-redefs-fn {#'clsql.migrator/rollback (fn [& {:keys [to]}] (is (nil? to)))}
      #(handle-command-line-args ["db-rollback"])))
  (testing "with valid arg"
    (with-redefs-fn {#'clsql.migrator/rollback (fn [& {:keys [to]}] (is (= "1290312" to)))}
      #(handle-command-line-args ["db-rollback" "1290312"]))))

(deftest test-migration-status
  (testing "call is made"
    (let [called (atom false)
          mocked-fn (fn [] (reset! called true))]
      (with-redefs-fn {#'clsql.migrator/migration-status mocked-fn}
        #(handle-command-line-args ["db-migration-status"]))
      (is @called))))

(deftest test-real-rollback
  (isolating-config
    (reset! config/database-configuration database-config)
    (reset-database!)
    (isolating-database
      (let [migration {:name    "test"
                       :path    "./test/fixtures/migration-statements.sql"
                       :version "27"}]
        (with-redefs-fn {#'clsql.migrator/discover-migrations (fn [] [migration])}
          #(do (handle-command-line-args ["db-migrate"])
               (handle-command-line-args ["db-rollback"])))))))
