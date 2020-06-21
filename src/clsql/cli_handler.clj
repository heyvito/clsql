(ns clsql.cli-handler
  (:require [clojure.string :as str]
            [clsql.migrator :as migrator]))

(defn- cli-error [command error]
  {:source  :clsql
   :command command
   :error   error
   :details "https://github.com/heyvito/clsql/wiki/errors"})

(defn- create-migration-missing-name []
  (throw (ex-info "clsql: Cannot create migration: Missing migration name"
                  (cli-error :create-migration "Missing migration name"))))

(defn- validate-migration-name! [name]
  (when-not (re-matches #"[a-zA-Z0-9-]+" name)
    (throw (ex-info (str "clsql: Cannot create migration: Migration names must be "
                         "composed of letters (a-z), numbers (0-9) and dashes "
                         "(-)")
                    (cli-error :create-migration "Invalid migration name")))))

(defn- validate-rollback-argument! [to]
  (when (and (not-empty to)
             (not (re-matches #"^\d+$" to)))
    (throw (ex-info (str "clsql: Cannot execute rollback: 'to' must be a version "
                         "number")
                    (cli-error :rollback-db "'to' must be a number")))))


(defn handle-command-line-args
  "Handles a list of arguments provided to the application. When arguments
  matches commands, this function will perform them and return `true` in order
  to indicate that a command was handled by it. Otherwise, `false` is returned.
  Commands handled by this function are:

  - create-migration NAME: Creates a new migration with NAME as its identifier
  - db-migrate: Executes all pending migrations in the configured database
  - db-rollback: Reverts the last applied migration
  - db-rollback VERSION: Reverts all migrations after VERSION
  - db-migration-status: Prints a table listing all migrations and their
  statuses

  When args is empty, defaults to clojure.core/*command-line-args*."
  [& args]
  (let [argv (if (empty? args) *command-line-args*
                               (first args))
        command (first argv)
        arguments (rest argv)]
    (if command
      (case (str/lower-case command)
        "create-migration" (let [[name] arguments]
                             (when-not name
                               (throw (create-migration-missing-name)))
                             (validate-migration-name! name)
                             (migrator/create-migration name))
        "db-migrate" (migrator/migrate)
        "db-rollback" (let [[to] arguments]
                        (validate-rollback-argument! to)
                        (migrator/rollback :to to))
        "db-migration-status" (migrator/migration-status)
        false)
      false)))
