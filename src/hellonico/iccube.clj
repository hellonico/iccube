(ns hellonico.iccube
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [puget.printer :as puget]))

(def base-url 
  (or (System/getenv "ICCUBE_SERVER") "http://localhost:8582/icCube/api/console/admin/"))

(defn fetch-and-parse-json [url]
  (let [response (client/get url {:as :json})
        body (:body response)]
    body))

(defn pandr
  ([res]
    (println (puget/cprint res))))

(defn request[command params]
  (let [
    url (str base-url command "?" params)
    data (fetch-and-parse-json url)]
    ;(pandr data)
    data))

(defn server-status[]
  (request "ServerStatus" "verbosity=quiet"))
(defn schema-info[schema]
  (request "SchemaInfo" (str "schemaName=" schema)))
(defn load-schema[schema]
  (request "LoadSchema" (str "schemaName=" schema)))
(defn unload-schema[schema]
  (request "UnloadSchema" (str "schemaName=" schema)))
(defn tidy-execute-mdx-script[schema script json flat]
  (request "TidyExecuteMdxScript"
    (str "schemaName=" schema "&" "script=" script "&" "json=" json "&flat=" flat)))
(defn discover-schema[schema]
  (request "DiscoverSchema"
    (str "schemaName=" schema)))
(defn list-schemas[]
  (request "ListSchemas" nil))
(defn list-schemas2[]
  (->> (list-schemas) :payload :rows (map :cells)))

(defn unloaded-schemas[] (request "UnloadedSchemas" nil))
(defn loaded-schemas[] (request "LoadedSchemas" nil))
(defn loading-schemas[] (request "LoadingSchemas" nil))
(defn offlines[] (request "Offlines" nil))

(defn mdx-from-file-or-not [query]
  (if (.exists (clojure.java.io/as-file query)) (slurp query) query))

(defn transform-data [data]
  (let [captions (mapv :caption data)
        values (mapv :values data)
        num-entries (apply max (map count values))]
    (mapv (fn [i]
            (into {}
                  (map (fn [[caption vs]]
                         [caption (get vs i)])
                       (zipmap captions values))))
          (range num-entries))))

(defn mdx[schema query]
 (tidy-execute-mdx-script schema (mdx-from-file-or-not query) true false))

(defn mdx3[schema query]
  (->> (tidy-execute-mdx-script schema (mdx-from-file-or-not query) true true) :payload :results first :dataSet :columns))

(defn mdx4[schema query]
  (transform-data (mdx3 schema (mdx-from-file-or-not query))))

(defn q [query] 
  (mdx4 (System/getenv "ICCUBE_SCHEMA") 
    (mdx-from-file-or-not query)))

