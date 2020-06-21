(defproject clsql "0.1.4"
  :description "clsql provides a small abstraction for handling SQL and migrations"
  :url "https://github.com/heyvito/clsql"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [instaparse "1.4.10"]]
  :plugins [[lein-cloverage "1.1.2"]]
  :repl-options {:init-ns clsql.core}
  :aliases {"create-migration" ["run" "-m" "clsql.migrator/create-migration"]}
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "42.2.12.jre7"]]}})
