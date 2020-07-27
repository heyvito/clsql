(ns clsql.instrumentation-test
  (:require [clojure.test :refer :all]
            [clsql.helpers :refer [import-private-functions
                                   reset-database!
                                   isolating-database
                                   with-database-config
                                   database-config
                                   instrumenting-with]]
            [clsql.migrator :refer :all]
            [clsql.core :refer [require-queries]]))

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
  (with-redefs-fn {#'clsql.instrumentation/now (constantly 0)}
    #(prepare-and
       (with-database-config database-config
         (let [result (atom nil)
               instrumenter (fn [event] (reset! result event))]
           (instrumenting-with instrumenter
             (q/insert-no-return :msg "This is a test"
                                 :kind "BAR"))
           (is (= @result {:started-at  0
                           :finished-at 0
                           :kind        :insert
                           :query       "INSERT INTO foos (message, kind) VALUES ( \"This is a test\" ,  \"BAR\"::SAMPLE_ENUM );"})))))))
