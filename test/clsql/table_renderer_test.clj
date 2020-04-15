(ns clsql.table-renderer-test
  (:require [clojure.test :refer :all]
            [clsql.table-renderer :refer [render-table]]))

(deftest without-rows
  (let [result (render-table ["Name" "Status"])
        expectation (str "╒══════╤════════╕\n"
                         "│ Name │ Status │\n"
                         "╞══════╧════════╡\n"
                         "╘═══════════════╛")]
    (is (= expectation result))))

(deftest with-rows
  (let [result (render-table ["Name" "Status"]
                             ["A long name" "OK"]
                             ["Second Row" "Not OK"])
        expectation (str "╒═════════════╤════════╕\n"
                         "│ Name        │ Status │\n"
                         "╞═════════════╪════════╡\n"
                         "│ A long name │ OK     │\n"
                         "│ Second Row  │ Not OK │\n"
                         "╘═════════════╧════════╛")]
    (is (= expectation result))))
