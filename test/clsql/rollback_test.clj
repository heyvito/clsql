(ns clsql.rollback-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clsql.migrator :refer :all]
            [clsql.config :as config]
            [clsql.helpers :refer [delete-recursive
                                   import-private-functions
                                   database-config
                                   databaseless-config
                                   tmp-dir
                                   isolating-database
                                   isolating-results
                                   isolating-config
                                   reset-database!]]
            [clojure.java.jdbc :as sql])
  (:import (clojure.lang ExceptionInfo)))


(import-private-functions clsql.migrator [discover-migrations
                                          get-migrations
                                          migrate-and-record])

(def fixed-timestamp 1579891417)

(defmacro mocking-time [& forms]
  `(with-redefs-fn {#'clsql.migrator/get-timestamp (constantly ~fixed-timestamp)}
     #(do ~@forms)))

(deftest test-migrate-and-record
  (testing "executes commands and records it"
    (reset-database!)
    (isolating-database
      (get-migrations database-config)
      (let [migration {:name    "test"
                       :path    "./test/fixtures/migration-up.sql"
                       :version "27"}]
        (migrate-and-record database-config migration :up)
        (migrate-and-record database-config migration :down)
        (is (empty? (get-migrations database-config)))))))

(deftest test-rollback
  (isolating-config
    (reset-database!)
    (isolating-database
      (reset! config/migrations-directory "./test/fixtures/rollback")
      (reset! config/database-configuration database-config)
      (migrate)
      (is ((get-migrations database-config) "15799040869"))
      (rollback)
      (is (empty? (get-migrations database-config))))))

(deftest test-out-of-sync
  (isolating-config
    (reset-database!)
    (isolating-database
      (reset! config/migrations-directory "./test/fixtures/rollback")
      (reset! config/database-configuration database-config)
      (is (empty? (get-migrations database-config)))
      (sql/insert! database-config "schema_migrations" [:version] ["24012020"])
      (is ((get-migrations database-config) "24012020"))
      (is (thrown? ExceptionInfo (rollback))))))

(deftest test-multi-rollback
  (isolating-config
    (reset-database!)
    (isolating-database
      (reset! config/migrations-directory "./test/fixtures/multi-rollback")
      (reset! config/database-configuration database-config)
      (migrate)
      (is (= 3 (count (get-migrations database-config))))
      (rollback :to 1)
      (is (= 1 (count (get-migrations database-config))))
      (is ((get-migrations database-config) "1")))))

(deftest test-wrong-rollback
  (isolating-config
    (reset-database!)
    (isolating-database
      (reset! config/migrations-directory "./test/fixtures/multi-rollback")
      (reset! config/database-configuration database-config)
      (migrate)
      (is (= 3 (count (get-migrations database-config))))
      (rollback :to 0)
      (is (= 3 (count (get-migrations database-config)))))))

(deftest test-multiple-statements
  (reset-database!)
  (isolating-database
    (get-migrations database-config)
    (let [migration {:name    "test"
                     :path    "./test/fixtures/migration-statements.sql"
                     :version "27"}]
      (migrate-and-record database-config migration :up)
      (is ((get-migrations database-config) "27"))
      (migrate-and-record database-config migration :down)
      (is (not ((get-migrations database-config) "27"))))))
