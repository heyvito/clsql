(ns clsql.quoting
  (:require [clojure.string :as str])
  (:import (java.sql Timestamp Time)
           (java.time LocalDateTime ZoneOffset)
           (java.text SimpleDateFormat)
           (java.util TimeZone)
           (java.time.format DateTimeFormatter)))

(def escape-str #(str/escape % {\\         "\\\\"
                                \"         "\\\""
                                \tab       "\\t"
                                \newline   "\\n"
                                \formfeed  "\\f"
                                \backspace "\\b"
                                \return    "\\r"}))
(declare quote-value)

(defn format-bytes [val]
  (quote-value (str "binary value with size " (count val))))

(def custom-fn-formatter (atom []))

(def datetime-formatter (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZ")
                          (.setTimeZone (TimeZone/getTimeZone "GMT"))))
(def time-formatter (doto (SimpleDateFormat. "HH:mm:ssZ")
                          (.setTimeZone (TimeZone/getTimeZone "GMT"))))
(defn format-datetime-object [val] (.format datetime-formatter val))
(defn format-time-object [val] (.format time-formatter val))

(defn format-unknown-instance
  ([val] (or (format-unknown-instance val @custom-fn-formatter)
             (str "UNKNOWN? " val)))
  ([val next]
   (if-let [[pred formatter] (first next)]
     (if (pred val) (formatter val)
                    (recur val (rest next))))))

(defn quote-value [val]
  (cond
    (true? val) "TRUE"
    (false? val) "FALSE"
    (nil? val) "NULL"
    (ratio? val) (format "%f" (double val))
    (number? val) val
    (string? val) (str \" (escape-str val) \")
    (bytes? val) (format-bytes val)
    (instance? LocalDateTime val) (.format val (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss'Z'"))
    (instance? Timestamp val) (format-datetime-object val)
    (instance? Time val) (format-time-object val)
    (inst? val) (format-datetime-object val)
    :else (format-unknown-instance val)))
