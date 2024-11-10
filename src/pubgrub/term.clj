(ns pubgrub.term
  (:require
   [clojure.string :as str]
   [pubgrub.version :as version]))

(defn make
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

(defn parse
  [s]
  (let [fields (str/split s #"\s+")
        length (count fields)
        negative? (= 3 length)
        package (if negative? (get fields 1) (get fields 0))
        raw-version (if negative? (get fields 2) (get fields 1))
        constraint (parse-constraint raw-version)
        version (version/parse (strip-constraint raw-version))]
    (make package version constraint negative?)))

;; (defn get-package
;;   [term]
;;   (term :package))

;; (defn get-version
;;   [term]
;;   (term :version))

;; (defn get-constraint
;;   [term]
;;   (term :contraint))

;; (defn get-negative?
;;   [term]
;;   (term :negative?))

;; (defn satisfies?
;;   [terms term]
;;   nil)

;; (defn contradicts?
;;   [terms term]
;;   nil)
