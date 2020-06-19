(ns clsql.migrator
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clsql.config :as config]
            [clsql.grammars.migration :as migration]
            [clsql.core :as core]
            [clojure.set :as set]
            [clsql.table-renderer :refer [render-table]])
  (:import (java.time Instant)
           (java.io File)))

(def migration-model "--;; up\n-- YOUR SQL HERE\n\n--;; down\n-- YOUR SQL HERE\n")

(defn- get-timestamp []
  (-> (Instant/now)
      (.getEpochSecond)))

(defn- ensure-migrations-directory []
  (let [path (io/file @config/migrations-directory)]
    (.mkdirs path)))

(defn create-migration
  "Creates a new migration in clsql.config/migrations-directory, identified
  by the provided name. Directories are created automatically, whenever needed."
  [name]
  (ensure-migrations-directory)
  (let [now (get-timestamp)
        migration-name (str now "-" name ".sql")
        base-path @config/migrations-directory
        migration-path (io/file base-path migration-name)]
    (spit migration-path migration-model)
    (println "Created:" migration-name)
    true))

(defn- get-migrations [db]
  (sql/db-do-commands db [(str "CREATE TABLE IF NOT EXISTS schema_migrations "
                               "(version character varying primary key)")])
  (sql/query db ["SELECT version FROM schema_migrations"]
             {:row-fn        :version
              :result-set-fn set}))

(defn- map-migration [^File f]
  (let [name (.getName f)
        path (.getPath f)]
    {:name    name
     :path    path
     :version (first (str/split name #"-"))}))

(defn- index-of [needle haystack]
  (->> haystack
       (keep-indexed #(when (= %2 needle) %1))
       (first)))

(defn- migrations-after [migrations version]
  (if-let [index (index-of (str version) migrations)]
    (reverse (subvec migrations (inc index)))
    nil))

(defn- migration-by-version [migrations version]
  (first (filter #(= version (:version %)) migrations)))

(defn- discover-migrations []
  (let [path @config/migrations-directory
        dir (io/file path)]
    (->> (file-seq dir)
         (filter #(and (.isFile %)
                       (str/ends-with? (.getName %) ".sql")))
         (map map-migration)
         (vec)
         (sort-by :version))))

(def splitter (re-pattern #"\s*;\s*(?=([^']*'[^']*')*[^']*$)"))
(defn- split-commands [cmd]
  (if (re-find splitter cmd)
    (loop [matcher (re-matcher splitter cmd)
           from 0
           statements []]
      (if (.find matcher)
        (let [to (.start matcher)
              new-statements (conj statements
                                   (subs cmd from to))]
          (recur matcher (inc to) new-statements))
        (map str/trim statements)))
    [cmd]))

(defn- migrate-and-record [tx mig direction]
  (println (if (= :up direction) "Executing" "Rolling-back") "migration" (:name mig))
  (let [m (migration/parse-migration (:path mig))
        version (:version mig)
        cmd (direction m)]
    (println "----------- 8< -----------\n\n"
             cmd
             "\n\n----------- 8< -----------\n\n")
    (sql/db-do-commands tx (split-commands cmd))
    (case direction
      :up (sql/insert! tx
                       "schema_migrations"
                       [:version]
                       [version])
      :down (sql/delete! tx
                         "schema_migrations"
                         ["version = ?" version]))))

(defn migrate
  "Applies all pending migrations into the configured database. Migrations
  are executed within a transaction, allowing the database to revert any changes
  in case of an exception"
  []
  (sql/with-db-transaction [tx (core/detect-database-config)]
    (let [has-run? (get-migrations tx)
          migrations (discover-migrations)]
      (when-let [to-apply (filter #(not (has-run? (:version %))) migrations)]
        (doseq [m to-apply]
          (migrate-and-record tx m :up))
        to-apply))
    true))


(defn rollback
  "Reverts one or more migrations. When :to is defined, reverts all migrations
  after the provided version. Otherwise, reverts the last migration."
  [& {:keys [to]}]
  (sql/with-db-transaction [tx (core/detect-database-config)]
    (let [executed-migrations (sort (get-migrations tx))
          local-migrations (discover-migrations)
          local-versions (set (map :version local-migrations))
          to-remove (if (nil? to)
                      [(last executed-migrations)]
                      (migrations-after (vec executed-migrations) to))
          missing (filter #(not (local-versions %)) to-remove)]
      (when (seq missing)
        (throw (ex-info (str "Cannot rollback: Local and remote migrations are "
                             "out of sync. Missing migration(s): "
                             (apply str (interpose \, missing)))
                        {:missing-migrations missing})))
      (when-let [definitions (map #(migration-by-version local-migrations %) to-remove)]
        (doseq [m definitions]
          (migrate-and-record tx m :down)))
      true)))

(defn migration-status
  "Prints the statuses of all migrations. Up indicates the migration has been
  applied, down indicates it is pending. Question marks indicates the migration
  exists on the database, but a matching migration file could not be found."
  []
  (sql/with-db-transaction [tx (core/detect-database-config)]
    (let [remote-migrations (set (get-migrations tx))
          all-local-migrations (discover-migrations)
          local-migrations (set (map :version all-local-migrations))
          named-migrations (apply hash-map (apply concat (map #(vector (get-in % [:version])
                                                                       (get-in % [:name])) all-local-migrations)))
          all-migrations (sort (set/union remote-migrations local-migrations))
          mapper (fn [version]
                   [(or (named-migrations version) version)
                    (cond
                      (and (local-migrations version)
                           (remote-migrations version)) "Up"
                      (and (local-migrations version)
                           (not (remote-migrations version))) "Down"
                      :else "?????")])
          statuses (map mapper all-migrations)]
      (println (apply render-table ["Migration" "Status"] statuses))))
  true)
