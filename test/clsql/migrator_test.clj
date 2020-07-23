(ns clsql.migrator-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clsql.migrator :refer :all]
            [clsql.config :as config]
            [clsql.helpers :refer [import-private-functions
                                   database-config
                                   tmp-dir
                                   isolating-database
                                   isolating-results
                                   isolating-config
                                   reset-database!
                                   execute-after]]
            [clojure.java.jdbc :as sql]))


(import-private-functions clsql.migrator [discover-migrations
                                          get-migrations
                                          migrate-and-record])

(def fixed-timestamp 1579891417)

(defmacro mocking-time [& forms]
  `(with-redefs-fn {#'clsql.migrator/get-timestamp (constantly ~fixed-timestamp)}
     #(do ~@forms)))

(deftest test-create-migration
  (isolating-results
    (mocking-time
      (create-migration "test")
      (let [value (slurp (str tmp-dir "/" fixed-timestamp "-test.sql"))]
        (is (= value clsql.migrator/migration-model))))))

(deftest test-discover-migrations
  (isolating-results
    (let [fname (str fixed-timestamp "-create-users.sql")
          in (io/file "./test/fixtures/migration-test.sql")
          out (io/file (str tmp-dir "/" fname))]
      (io/copy in out)
      (let [data (discover-migrations)]
        (is (= data [{:name    fname
                      :path    (str tmp-dir "/" fname)
                      :version (str fixed-timestamp)}]))))))

(deftest test-schema-table
  (testing "creates schema table on use"
    (reset-database!)
    (isolating-database
      (let [migs (get-migrations database-config)]
        (is (empty? migs))))))

(deftest test-migrate-and-record
  (testing "executes commands and records it"
    (reset-database!)
    (isolating-database
      (get-migrations database-config)
      (let [migration {:name    "test"
                       :path    "./test/fixtures/migration-up.sql"
                       :version "27"}]
        (migrate-and-record database-config migration :up)
        (is ((get-migrations database-config) "27"))))))

(deftest test-migrate
  (isolating-config
    (reset-database!)
    (isolating-database
      (reset! config/migrations-directory "./test/fixtures/migrations")
      (reset! config/database-configuration database-config)
      (migrate)
      (is ((get-migrations database-config) "1579904869")))))

(deftest test-migration-statuses
  (execute-after (io/delete-file "./test/fixtures/migrations/1579904871-test.sql" :silently)
    (isolating-config
      (reset-database!)
      (isolating-database
        (reset! config/migrations-directory "./test/fixtures/migrations")
        (reset! config/database-configuration database-config)
        (migrate)
        (spit "./test/fixtures/migrations/1579904871-test.sql" "HELLO!")
        (sql/insert! database-config "schema_migrations" [:version] ["1579904870"])
        (let [result (with-out-str (migration-status))
              expectation (str "╒═════════════════════════════════╤════════╕\n"
                               "│ Migration                       │ Status │\n"
                               "╞═════════════════════════════════╪════════╡\n"
                               "│ 1579904869-create-something.sql │ Up     │\n"
                               "│ 1579904870                      │ ?????  │\n"
                               "│ 1579904871-test.sql             │ Down   │\n"
                               "╘═════════════════════════════════╧════════╛\n")]
          (is (= expectation result)))))))

(deftest test-multiple-statements
  (reset-database!)
  (isolating-database
    (get-migrations database-config)
    (let [migration {:name    "test"
                     :path    "./test/fixtures/migration-statements.sql"
                     :version "27"}]
      (migrate-and-record database-config migration :up)
      (is ((get-migrations database-config) "27")))))
