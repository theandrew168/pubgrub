(ns pubgrub.core
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [org.httpkit.client :as hk-client]))

;;;; References:
;;;; https://nex3.medium.com/pubgrub-2fb6470504f
;;;; https://github.com/dart-lang/pub/blob/master/doc/solver.md

;;;; Let's start with semvers of any length (any number of segments separated by a period).
;;;; Ignore prereleases, release candiates, and things like that (for now). So, a version
;;;; could be something like "1.2" or "3.4.5". Once parsed, we can think of versions as a
;;;; sequence of integers. In this model, "1.2" becomes [1, 2] and "3.4.5" becomes [3, 4, 5].

;;;; The fundamental unit of the PubGrub solver is a "Term". A term consists of:
;;;; 1. A package name (required)
;;;; 2. A package version (required)
;;;; 3. A matching constraint (optional, default is exact)
;;;;    (exact, major (caret), minor (tilde), or range (inequality))
;;;; 4. A negation (optional)

;;;; Terms often come in sets. For a given set of a terms, it either "satisfies" (all
;;;; terms are true), "contradicts" (all terms are false), or is "inconclusive" (if neither
;;;; of the other cases are true) for another term.

;;;; In general, the algorithm deals with "incompatibilities": sets of terms that cannot
;;;; ALL be true. It builds a "derivation graph" (directed, acyclic, and binary) of these
;;;; incompatibilities. Through phases of "unit propagation", "conflict resolution", and
;;;; "decision making", a solution is hopefully found (complete, partial, or none).

(defn make-version
  "Make a new version (sequence of integers)."
  [s]
  (map #(Integer/parseInt %) (str/split s #"\.")))

(defn extend-version
  "Extend a version out to N segments."
  [v n]
  (let [lv (count v)]
    (concat v (take (- n lv) (repeat 0)))))

(defn zip-versions
  "Given two versions, extend them to match lengths and group into pairs."
  [a b]
  (let [n (max (count a) (count b))
        aa (extend-version a n)
        bb (extend-version b n)]
    (map vector aa bb)))

(defn equal-versions?
  "Compare if two versions are equal."
  [a b]
  (let [z (zip-versions a b)]
    (every? #(apply = %) z)))

(defn npm-registry-url
  []
  "https://registry.npmjs.org")

(defn npm-package-url
  [package]
  (format "%s/%s" (npm-registry-url) package))

(defn npm-package-version-url
  [package version]
  (format "%s/%s/%s" (npm-registry-url) package version))

(defn npm-package-version-request
  [package version]
  {:url (npm-package-version-url package version)
   :method :get})

(defn fetch!
  [req]
  (let [resp (hk-client/request req)]
    @resp))

(comment

  (concat [1 2 3] (take 2 (repeat 0)))
  (take 4 (repeat 0))
  (conj [1 2 3] 0)
  (count [1 2 3])

  (make-version "1.2.3")
  (extend-version (make-version "1.2.3") 5)
  (extend-version (make-version "1.2.3") 3)
  (zip-versions (make-version "1") (make-version "1.2.3"))
  (equal-versions? (make-version "1") (make-version "1.0.0"))
  (equal-versions? (make-version "1") (make-version "1.0.1"))

  (npm-registry-url)
  (npm-package-url "tar")
  (npm-package-version-url "tar" "7.4.3")
  (npm-package-version-request "tar" "7.4.3")

  (def req (npm-package-version-request "tar" "7.4.3"))
  req

  (def resp (fetch! req))
  resp

  (def body (resp :body))
  body

  (def info (json/read-str body))
  info
  (info "dependencies")

  :rcf)
