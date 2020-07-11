(ns clsql.query-execution-test
  (:require [clojure.test :refer :all]
            [clsql.migrator :refer :all]
            [clsql.core :refer [require-queries]]
            [clsql.helpers :refer [delete-recursive
                                   import-private-functions
                                   database-config
                                   databaseless-config
                                   tmp-dir
                                   isolating-database
                                   isolating-results
                                   isolating-config
                                   reset-database!
                                   execute-after]]))

(import-private-functions clsql.migrator [get-migrations
                                          migrate-and-record])
(ns-unalias *ns* 'q)
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
    (q/insert-no-return database-config {:msg "This is a test"
                                          :kind "BAR"})
    (is (= {:id 1 :message "This is a test", :kind "BAR"} (first (q/get-first database-config nil))))
    (q/delete-by-id database-config {:id 1})
    (is (= {:id 2} (q/insert-with-return database-config {:msg "This is another test", :kind "BAR"})))))
