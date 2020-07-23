(ns clsql.query-execution-test
  (:require [clojure.test :refer :all]
            [clsql.migrator :refer :all]
            [clsql.core :refer [require-queries]]
            [clsql.helpers :refer [import-private-functions
                                   database-config
                                   isolating-database
                                   reset-database!
                                   with-database-config]]))

(import-private-functions clsql.migrator [get-migrations
                                          migrate-and-record])
(require-queries [execution :as q])

(defmacro prepare-and [& body]
  `(do
     (reset-database!)
     (isolating-database
       (get-migrations ~database-config)
       (let [migration# {:name    "test"
                         :path    "./test/fixtures/migration-statements.sql"
                         :version "27"}]
         (migrate-and-record ~database-config migration# :up)
         (is ((get-migrations ~database-config) "27")))
       ~@body)))

(deftest test-simple-insert-no-return
  (prepare-and
    (with-database-config database-config
      (q/insert-no-return :msg "This is a test"
                          :kind "BAR")
      (is (= {:id      1
              :message "This is a test"
              :kind    "BAR"} (first (q/get-first))))
      (q/delete-by-id :id 1)
      (is (= {:id 2} (q/insert-with-return :msg "This is another test"
                                           :kind "BAR"))))))
