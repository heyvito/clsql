(ns clsql.codegen
  (:require [clsql.errors :refer [throw-if]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]))

; This is a really naive detection process. We will simply check the first
; token and try to guess which kind of query we're running here. This may
; be overridden by query metadata.
;(defn- detect-query-kind [body]
;  (if-let [[kind value] (first body)]
;    (when (= :token kind)
;      (let [inst (str/lower-case value)]
;        (condp str/starts-with? inst
;          "insert" :insert
;          "update" :update
;          "delete" :delete
;          :select)))))

(defn- compose-query [body]
  (loop [body body
         result {:query [] :params []}]
    (if-let [entry (first body)]
      (let [[k v] entry
            rest (rest body)]
        (case k
          :token (recur rest (update result :query #(conj % v)))
          :placeholder (recur rest (-> result
                                       (update :query #(conj % \?))
                                       (update :params #(conj % v))))
          (recur rest result)))
      (update result :query (fn [v] (apply str (interpose \space v)))))))

(defmacro make-validator [placeholders args]
  (when placeholders
    `(do
       (throw-if (and
                   (not (nil? ~args))
                   (not (map? ~args)))
                 (str "args must be a map. Received " (type ~args) " instead."))
       (let [existing-keys# (set (keys ~args))
             missing-keys# (seq (remove existing-keys# ~placeholders))]
         (throw-if missing-keys#
                   (apply str "Missing parameter(s): "
                          (interpose \, missing-keys#)))))))

(defmacro create-result-processor [modifiers result]
  (cond
    (set/subset? #{:single-result} modifiers) `(first ~result)
    :else result))

(defmacro query-fn [spec]
  (let [{:keys [placeholders name modifiers]} (:header spec)
        raw-body (:body spec)
        {:keys [query params]} (compose-query raw-body)]
    (when name
      `(fn ~(symbol name) [db# args# & opts#]
         (make-validator ~placeholders args#)
         (let [ordered-opts# (map #(% args#) ~params)
               jdbc-arguments# (cons ~query ordered-opts#)
               result# (jdbc/query db# jdbc-arguments# opts#)]
           (create-result-processor ~modifiers result#))))))

(defmacro create-query-in-ns [spec *ns*]
  (let [{:keys [name docstring]} (:header spec)
        args (quote ['db 'args '& 'opts])
        base-args {:name     name
                   :arglists `(list ~args)}
        meta (merge base-args
                    (when docstring
                      {:doc docstring}))]
    `(intern ~*ns* (with-meta (symbol ~name) ~meta)
             (query-fn ~spec))))

(defmacro create-query [spec]
  `(create-query-in-ns ~spec *ns*))
