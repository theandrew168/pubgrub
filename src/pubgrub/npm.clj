(ns pubgrub.npm
  (:require
   [pubgrub.fetch :as fetch]
   [pubgrub.registry :as registry]))

(defn- registry-url
  []
  "https://registry.npmjs.org")

(defn- package-url
  [package]
  (format "%s/%s" (registry-url) package))

(defn- package-request
  [package]
  {:url (package-url package)
   :method :get})

(defn- package-versions
  [package-body]
  (keys (package-body "versions")))

(defn- package-version-url
  [package version]
  (format "%s/%s/%s" (registry-url) package version))

(defn- package-version-request
  [package version]
  {:url (package-version-url package version)
   :method :get})

(defn- package-version-dependencies
  [package-version-body]
  (package-version-body "dependencies"))

(deftype NPMRegistry []
  registry/Registry
  (package-versions
    [_ package]
    (let [req (package-request package)
          resp (fetch/fetch! req)
          body (fetch/parse-body resp)]
      (package-versions body)))
  (package-version-dependencies
    [_ package version]
    (let [req (package-version-request package version)
          resp (fetch/fetch! req)
          body (fetch/parse-body resp)]
      (package-version-dependencies body))))

(defn new-registry
  "Convenience function for creating a new NPM Registry."
  []
  (NPMRegistry.))

(comment

  (registry-url)
  (package-url "tar")
  (package-request "tar")
  (package-version-url "tar" "7.4.3")
  (package-version-request "tar" "7.4.3")

  (def reg (new-registry))
  (registry/package-versions reg "tar")
  (registry/package-version-dependencies reg "tar" "7.4.3")

  :rcf)
