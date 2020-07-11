(ns clsql.codegen
  (:require [clsql.errors :refer [throw-if]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.string :as str]))

; This is a really naive detection process. We will simply check the first
; token and try to guess which kind of query we're running here. This may
; be overridden by query metadata.
(defn- detect-query-kind [body]
  (condp #(str/starts-with? %2 %1) (str/lower-case body)
    "insert" :insert
    "update" :update
    "delete" :delete
    :select))

(def return-regexp (re-pattern #"\s*RETURNING\s*(?=([^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)"))
(defn query-returns? [query]
  (->> query
       (re-find return-regexp)
       (seq)
       (some?)))

(defn- prepare-query [body]
  (loop [body body
         result {:query  []
                 :params []}]
    (if-let [[k & vals] (first body)]
      (recur (rest body)
             (case k
               :token (update result :query #(conj % (apply str vals)))
               :placeholder (-> result
                                (update :query #(conj % (str "?" (apply str (rest vals)))))
                                (update :params #(conj % (first vals))))
               result))
      (update result :query #(apply str (interpose \space %))))))

(defn- compose-query [body]
  (let [r (prepare-query body)
        q (:query r)
        returns? (query-returns? q)
        kind (detect-query-kind q)]
    (-> r
        (assoc :returns? returns?)
        (assoc :kind kind))))

(defmacro make-validator [placeholders args]
  (when (seq placeholders)
    `(do
       (throw-if (and
                   (not (nil? ~args))
                   (not (map? ~args)))
                 (str "args must be a map. Received " (type ~args) " instead."))
       (let [existing-keys# (set (keys ~args))
             missing-keys# (seq (remove existing-keys# ~placeholders))]
         (throw-if missing-keys#
                   (apply str "Missing arg(s): "
                          (interpose ", " missing-keys#)))))))

(defn- pick-query-method [kind returns?]
  (if (or (= :select kind)
          returns?)
    #'jdbc/query
    #'jdbc/execute!))

(defmacro create-result-processor [modifiers result]
  (cond
    (set/subset? #{:single-result} modifiers) `(first ~result)
    :else result))

(defmacro query-fn [spec]
  (let [{:keys [placeholders name modifiers]} (:header spec)
        raw-body (:body spec)
        {:keys [query params returns? kind]} (compose-query raw-body)
        query-method (pick-query-method kind returns?)]
    (when name
      `(fn fn-name#
         ([db#] (fn-name# db# {}))
         ([db# args# & opts#]
          (make-validator ~placeholders args#)
          (let [ordered-opts# (map #(% args#) ~params)
                jdbc-arguments# (cons ~query ordered-opts#)
                result# ((var-get ~query-method) db# jdbc-arguments# opts#)]
            (create-result-processor ~modifiers result#)))))))

(defn- reformat-docstring [& lines]
  (let [all-lines (->> lines
                       (remove nil?)
                       (flatten))]
    (loop [lines (rest all-lines)
           l [(first all-lines)]]
      (if-let [line (first lines)]
        (recur (rest lines)
               (conj l (if (= line :break)
                         (str "")
                         (str "  " line))))
        (apply str (interpose \newline l))))))

(defn- make-docstring [docstring placeholders]
  (reformat-docstring
    (when docstring [docstring :break])
    (when (seq placeholders)
      [(str "Required arg" (when (> (count placeholders) 1) "s") ": "
            (apply str (interpose ", " placeholders)))
       :break])
    "Arguments"
    "---------"
    "db must be a database spec or transaction, as defined by clojure.jdbc"
    "args must be a map of required arguments for this query"
    "opts may be a list of options to be passed to clojure.jdbc"))



(defmacro create-query-in-ns [spec *ns*]
  (let [{:keys [name docstring placeholders]} (:header spec)
        args (quote ['db 'args? '& 'opts*])
        meta {:name     name
              :arglists `(list ~args)
              :doc      (make-docstring docstring placeholders)}]
    `(intern ~*ns* (with-meta (symbol ~name) ~meta)
             (query-fn ~spec))))

(defmacro create-query [spec]
  `(create-query-in-ns ~spec *ns*))
