(ns clsql.errors
  (:require [instaparse.core :as insta]
            [instaparse.failure :as fail]
            [instaparse.print :as print])
  (:import (java.util.regex Pattern)))

(defn throw-if
  "Throws a CompilerException with a message if pred is true"
  [pred fmt & args]
  (when pred
    (let [^String message (apply format fmt args)
          exception (IllegalArgumentException. message)
          raw-trace (.getStackTrace exception)
          boring? #(not= (.getMethodName ^StackTraceElement %) "doInvoke")
          trace (into-array StackTraceElement (drop 2 (drop-while boring? raw-trace)))]
      (.setStackTrace exception trace)
      (throw exception))))

(defn- format-reason [r]
  (cond
    (:NOT r) (str "NOT " (:NOT r))
    (:char-range r) (print/char-range->str r)
    (instance? Pattern r) (print/regexp->str r)
    :else (pr-str r)))

(defn- format-failure [file failure]
  (let [{:keys [line column text reason]} failure
        full-reasons (distinct (map :expecting
                                    (filter :full reason)))
        partial-reasons (distinct (map :expecting
                                       (filter (complement :full) reason)))
        total (+ (count full-reasons) (count partial-reasons))
        result [(str "Error parsing " file " at line " line " column " column ":")
                text
                (fail/marker column)
                (cond (zero? total) nil
                      (= 1 total) "Expected:"
                      :else "Expected one of:")]]
    (apply str
           (interpose \newline
                      (-> result
                          (conj (map #(str (format-reason %) " (followed by end-of-string)") full-reasons))
                          (conj (map format-reason partial-reasons))
                          (flatten))))))

(defn parse-or-die [file result]
  (when (insta/failure? result)
    (throw (ex-info (format-failure file (insta/get-failure result))
                    {:source :clsql
                     :file   file})))
  result)
