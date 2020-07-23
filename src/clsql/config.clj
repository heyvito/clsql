(ns clsql.config
  (:require [clojure.string :as str])
  (:import (java.io File)))

(def base-path (-> (File. ".") .getAbsolutePath))
(def migrations-directory (atom (str base-path "/resources/db/migrations")))
(def queries-directory (atom (str base-path "/resources/db/queries")))
(def database-configuration (atom nil))
(def instrumentation-handler (atom nil))

(defn- get-env []
  (System/getenv))

(defn- keywordize [k]
  (keyword (str/replace k #"_" "-")))

(defn- normalize-database-uri [coll]
  (if-let [uri (:connection-uri coll)]
    (assoc coll :connection-uri
                (str (when-not (str/starts-with? uri "jdbc:") "jdbc:") uri))
    coll))

(defn- find-env-keys []
  (let [vals (->> (get-env)
                  (map (fn [[k v]] [(str/lower-case k) v]))
                  (filter (fn [[k _]] (str/starts-with? k "clsql")))
                  (map (fn [[k v]] [(str/replace k #"clsql_" "") v]))
                  (map (fn [[k v]] [(keywordize k) v]))
                  (into {}))]
    (when (seq vals)
      (normalize-database-uri vals))))

(defn detect-database-config []
  (or @database-configuration
      (reset! database-configuration (find-env-keys))))

(defn detect-database-config! []
  (or (detect-database-config)
      (throw (ex-info "clsql: Can't find database configuration"
                      {:error   "No database configuration"
                       :details "https://github.com/heyvito/clsql/wiki/errors"}))))
