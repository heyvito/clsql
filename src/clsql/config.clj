(ns clsql.config
  (:import (java.io File)))

(def base-path (-> (File. ".") .getAbsolutePath))
(def migrations-directory (atom (str base-path "/resources/db/migrations")))
(def queries-directory (atom (str base-path "/resources/db/queries")))
(def database-configuration (atom nil))
