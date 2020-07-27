(ns clsql.helpers
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clsql.config :as config]
            [clsql.instrumentation :as instrumentation]
            [clojure.java.jdbc :as sql])
  (:import (java.io File)))

(def database-config {:dbtype         "postgres"
                      :connection-uri "jdbc:postgresql://127.0.0.1:5432/clsql?user=postgres&password=postgres&sslmode=disable"})

(def databaseless-config {:dbtype         "postgres"
                          :connection-uri (str/replace (:connection-uri database-config) #"(.*[^/]+)/([^?]+)?(.*)" "$1/$3")})

(def tmp-dir "./test/tmp")

(defmacro import-private-functions [from & forms]
  (let [def-symbol #(symbol (str (name from) "/" (name %)))
        mapper (fn [n] `(def ~n (var ~(def-symbol n))))
        items (map mapper (flatten forms))]
    `(do ~@items)))

(defn delete-recursive
  "Recursively delete a directory."
  [^File file]
  (doseq [file-in-dir (.listFiles file)
          :when (not= ".gitkeep" (.getName file-in-dir))]
    (io/delete-file file-in-dir :silently)))

(defn reset-database! []
  (sql/db-do-commands databaseless-config false ["DROP DATABASE IF EXISTS clsql;"
                                                 "CREATE DATABASE clsql;"])
  true)

(defmacro isolating-database [& forms]
  `(try
     ~@forms
     (finally
       (reset-database!))))

(defmacro isolating-results [& forms]
  `(let [old-value# @config/migrations-directory]
     (reset! config/migrations-directory tmp-dir)
     (try
       ~@forms
       (finally
         (delete-recursive (io/file tmp-dir))
         (reset! config/migrations-directory old-value#)))))

(defmacro isolating-config [& forms]
  `(let [m-d# @config/migrations-directory
         d-c# @config/database-configuration]
     (try
       ~@forms
       (finally
         (reset! config/migrations-directory m-d#)
         (reset! config/database-configuration d-c#)))))

(defmacro execute-after [fn & forms]
  `(try
     ~@forms
     (finally
       ~fn)))

(defmacro with-database-config [database-config & forms]
  `(let [old-c# @config/database-configuration]
     (reset! config/database-configuration database-config)
     (try
       ~@forms
       (finally
         (reset! config/database-configuration old-c#)))))

(defmacro with-databaseless-config [& forms]
  `(with-database-config databaseless-config ~@forms))

(defmacro instrumenting-with [fn & forms]
  `(let [old-handler# @instrumentation/handler]
     (reset! instrumentation/handler ~fn)
     (try
       ~@forms
       (finally (reset! instrumentation/handler old-handler#)))))
