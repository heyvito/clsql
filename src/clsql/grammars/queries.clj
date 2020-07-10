(ns clsql.grammars.queries
  (:require [instaparse.core :as insta]
            [instaparse.transform :refer [transform]]
            [clojure.java.io :as io]
            [clsql.errors :as errors]))

(defmacro bnf-contents []
  (slurp (io/resource "clsql/grammars/queries.bnf")))
(def parser (insta/parser (bnf-contents)))


(def normalize-file {:query-body   (fn [& i] {:body (vec i)})
                     :query-header (fn [& i] {:header (vec i)})
                     :query-name   (fn [n] [:query-name (keyword n)])
                     :modifiers    (fn [& mod] [:modifiers (set (map keyword mod))])
                     :placeholder  (fn [p] [:placeholder (keyword p)])
                     :query-entry  merge
                     :queries-file #(vec %&)})

(defn- prepare-header
  ([header] (prepare-header header {}))
  ([header result]
   (if-let [item (first header)]
     (let [[k v] item]
       (case k
         :query-name (recur (rest header) (assoc result :name v))
         :modifiers (recur (rest header) (assoc result k v))
         :docstring (let [curr-docstring (:docstring result)
                          new-docs (str (when curr-docstring (str curr-docstring " ")) v)]
                      (recur (rest header) (assoc result :docstring new-docs)))
         (recur (rest header) result)))
     (merge result))))

(defn- detect-placeholders [body]
  (let [placeholders (filter #(= :placeholder (first %)) body)]
    (apply vector (->> placeholders
                       (map second)
                       (map keyword)))))

(defn- normalize-entries [entries]
  (map (fn [{:keys [header body]}]
         (let [p-header (prepare-header header)]
           {:header (merge p-header
                           {:placeholders (detect-placeholders body)})
            :body   body})) entries))

(defn parse-queries [path]
  (->> (parser (slurp path))
       (errors/parse-or-die path)
       (transform normalize-file)
       (normalize-entries)))
