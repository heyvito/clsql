(ns clsql.grammars.migration
  (:require [instaparse.core :as insta]
            [instaparse.transform :refer [transform]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defmacro bnf-contents []
  (slurp (io/resource "clsql/grammars/migration.bnf")))
(def parser (insta/parser (bnf-contents)))

(defn- extract-from-ast [k ast]
  (let [[curr-k val] ast]
    (if (= k curr-k) val
                     (throw (ex-info (str "Format violation: Expected " k
                                          " got " curr-k)
                                     {:expectation k
                                      :current-key curr-k})))))

(defn- normalize-body [& body]
  (->> body
       (map (partial extract-from-ast :sql))
       (str/join "\n")
       (list :sql)
       (apply hash-map)))

(defn- normalize-header [[_ dir]] {:direction (keyword dir)})

(def entry-normalizer (partial transform {:migration-entry  merge
                                          :migration-header normalize-header
                                          :migration-body   normalize-body}))

(def migration-normalizer {:migration-file #(map entry-normalizer %&)})

(defn- find-command [cmd]
  (fn [entries]
    (:sql (first (filter #(= (:direction %) cmd) entries)))))

(defn parse-migration [path]
  (let [migration (->> (parser (slurp path))
                       (transform migration-normalizer))]
    {:up   ((find-command :up) migration)
     :down ((find-command :down) migration)}))
