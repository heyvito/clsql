(ns clsql.quoting-test
  (:require [clojure.test :refer :all]
            [clsql.quoting :refer :all])
  (:import (clojure.lang IAtom)
           (java.util Date TimeZone)
           (java.text SimpleDateFormat)
           (java.sql Timestamp Time)
           (java.time LocalDateTime)))

(deftest test-format-bytes
  (is (= "\"binary value with 2 bytes\"")
      (format-bytes "12")))

(deftest test-format-unknown-instance
  (testing "with an unknown instance"
    (let [a (atom nil)]
      (is (= (str "UNKNOWN? " a)
             (format-unknown-instance a)))))
  (testing "with a known instance"
    (let [other-pred (constantly false)
          other-formatter (constantly "Error")
          pred (partial instance? IAtom)
          formatter (constantly "A little Atom")
          a (atom nil)]
      (with-redefs [clsql.quoting/custom-fn-formatter (atom [[other-pred other-formatter]
                                                             [pred formatter]])]
        (is (= (str "A little Atom")
               (format-unknown-instance a)))))))

(deftest test-format-time
  (let [time (.parse (doto (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
                       (.setTimeZone (TimeZone/getTimeZone "GMT"))) "2020-01-24 14:27:30")]
    (is (= "2020-01-24T14:27:30+0000"
           (format-datetime-object time)))))

(defmacro quot-compare [name in out]
  `(testing ~name (is (= ~out (quote-value ~in)))))

(deftest test-quote-value
  (quot-compare "true" true "TRUE")
  (quot-compare "false" false "FALSE")
  (quot-compare "nil" nil "NULL")
  (quot-compare "ratio" 1/3 "0.333333")
  (quot-compare "number" 24 24)
  (quot-compare "bytes" (.getBytes "foo") "\"binary value with size 3\"")
  (quot-compare "timestamp" (Timestamp. 1579876050000) "2020-01-24T14:27:30+0000")
  (quot-compare "time" (Time. 52050000) "14:27:30+0000")
  (quot-compare "instant" #inst"2020-01-24T14:27:30.000-00:00" "2020-01-24T14:27:30+0000")
  (quot-compare "local date time" (LocalDateTime/of 2020 01 24 14 27 30) "2020-01-24T14:27:30Z")
  (let [a (atom nil)]
    (quot-compare "unknown" a (str "UNKNOWN? " a))))
