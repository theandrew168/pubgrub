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

(defn parse-version
  "Parse a new version (sequence of integers)."
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

(defn make-term
  [package version constraint negative?]
  {:package package
   :version version
   ; :exact :major :minor :lt :lte :gt :gte
   :constraint constraint
   :negative? negative?})

(defn parse-constraint
  [s]
  (cond
    (str/starts-with? s "^") :major
    (str/starts-with? s "~") :minor
    (str/starts-with? s "<=") :lte
    (str/starts-with? s ">=") :gte
    (str/starts-with? s "<") :lt
    (str/starts-with? s ">") :gt
    :else :exact))

(defn strip-constraint
  [s]
  (let [constraint (parse-constraint s)]
    (case constraint
      :major (subs s 1)
      :minor (subs s 1)
      :lte (subs s 2)
      :gte (subs s 2)
      :lt (subs s 1)
      :gt (subs s 1)
      :exact s)))

(defn parse-term
  [s]
  (let [fields (str/split s #"\s+")
        length (count fields)
        negative? (= 3 length)
        package (if negative? (get fields 1) (get fields 0))
        raw-version (if negative? (get fields 2) (get fields 1))
        constraint (parse-constraint raw-version)
        version (parse-version (strip-constraint raw-version))]
    (make-term package version constraint negative?)))

(defn term-package
  [term]
  (term :package))

(defn term-version
  [term]
  (term :version))

(defn term-constraint
  [term]
  (term :contraint))

(defn term-negative?
  [term]
  (term :negative?))

(defn term-satisfies?
  [terms term]
  nil)

(defn term-contradicts?
  [terms term]
  nil)

(defn npm-registry-url
  []
  "https://registry.npmjs.org")

(defn npm-package-url
  [package]
  (format "%s/%s" (npm-registry-url) package))

(defn npm-package-request
  [package]
  {:url (npm-package-url package)
   :method :get})

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

(def fetch-memo! (memoize fetch!))

(defn fetch-npm-package!
  [package]
  (let [req (npm-package-request package)
        resp (fetch-memo! req)]
    (json/read-str (resp :body))))

(defn fetch-npm-package-version!
  [package version]
  (let [req (npm-package-version-request package version)
        resp (fetch-memo! req)]
    (json/read-str (resp :body))))

(defn deps!
  [package version]
  (println package version)
  (let [info (fetch-npm-package-version! package version)
        deps (info "dependencies")]
    (doseq [[k v] deps]
      (deps! k (strip-constraint (get (str/split v #"\s+") 0))))))

(comment

  (concat [1 2 3] (take 2 (repeat 0)))
  (take 4 (repeat 0))
  (conj [1 2 3] 0)
  (count [1 2 3])

  (parse-version "1.2.3")
  (extend-version (parse-version "1.2.3") 5)
  (extend-version (parse-version "1.2.3") 3)
  (zip-versions (parse-version "1") (parse-version "1.2.3"))
  (equal-versions? (parse-version "1") (parse-version "1.0.0"))
  (equal-versions? (parse-version "1") (parse-version "1.0.1"))

  (make-term "tar" "7.4.3" :exact false)
  (parse-term "tar 7.4.3")
  (parse-term "tar ^7.4.3")
  (parse-term "not tar ~7.4.3")

  (npm-registry-url)
  (npm-package-url "tar")
  (npm-package-request "tar")
  (npm-package-version-url "tar" "7.4.3")
  (npm-package-version-request "tar" "7.4.3")

  (fetch-npm-package! "tar")
  (fetch-npm-package-version! "tar" "7.4.3")
  (deps! "tar" "7.4.3")

  :rcf)
