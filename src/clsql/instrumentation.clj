(ns clsql.instrumentation
  (:require [clsql.quoting :refer [quote-value]]))

(defn now [] (System/currentTimeMillis))

(defn- process-part [[kind val cast] params]
  (case kind
    :token val
    :placeholder (str (quote-value (get params val)) cast)))

(defn- create-faux-query [[body params]]
  (loop [body body
         result []]
    (if-let [val (first body)]
      (recur (rest body)
             (conj result (process-part val params)))
      (apply str (interpose \space result)))))

(def handler (atom nil))

(defn emit-instrumentation-event [data]
  (if-let [hand @handler]
    (hand (update data :query create-faux-query))))

(defmacro instrument [[spec params kind] & forms]
  `(let [start# (now)]
     (try
       ~@forms
       (finally
         (emit-instrumentation-event {:started-at  start#
                                      :finished-at (now)
                                      :kind        ~kind
                                      :query       [(:body ~spec) ~params]})))))
