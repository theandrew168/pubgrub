(ns pubgrub.fetch
  (:require
   [clojure.data.json :as json]
   [org.httpkit.client :as hk-client]))

(defn- fetch-sync!
  "Synchronously fetch a request and wait for a response."
  [req]
  (let [resp (hk-client/request req)]
    @resp))

(def fetch!
  "Execute (and memoize) the fetch for a given request."
  (memoize fetch-sync!))

(defn parse-body
  [resp]
  (json/read-str (resp :body)))
