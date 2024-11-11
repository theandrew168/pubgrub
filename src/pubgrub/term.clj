(ns pubgrub.term
  (:require
   [clojure.string :as str]
   [pubgrub.version :as version]))

(defn- make
  [package version constraint positive?]
  {:package package
   :version version
   ; :exact :major :minor :lt :lte :gt :gte
   :constraint constraint
   :positive? positive?})

(defn- get-package
  [t]
  (t :package))

(defn- get-version
  [t]
  (t :version))

(defn- get-constraint
  [t]
  (t :constraint))

(defn- get-positive?
  [t]
  (t :positive?))

(defn- parse-constraint
  [s]
  (cond
    (str/starts-with? s "^") :major
    (str/starts-with? s "~") :minor
    (str/starts-with? s "<=") :lte
    (str/starts-with? s ">=") :gte
    (str/starts-with? s "<") :lt
    (str/starts-with? s ">") :gt
    :else :exact))

(defn- strip-constraint
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
        positive? (< length 3)
        package (if positive? (get fields 0) (get fields 1))
        raw-version (if positive? (get fields 1) (get fields 2))
        constraint (parse-constraint raw-version)
        version (strip-constraint raw-version)]
    (make package version constraint positive?)))

(defn- includes-version?
  [tc tv v]
  (case tc
    :major (and (version/greater-than-or-equal? v tv)
                (= (get v 0) (get tv 0)))
    :minor (and (version/greater-than-or-equal? v tv)
                (= (get v 0) (get tv 0))
                (= (get v 1) (get tv 1)))
    :lte (version/less-than-or-equal? v tv)
    :gte (version/greater-than-or-equal? v tv)
    :lt (version/less-than? v tv)
    :gt (version/greater-than? v tv)
    :exact (version/equal? v tv)))

(defn includes?
  "Check if a term includes a specific version."
  [t v]
  (let [t (parse t)
        tc (get-constraint t)
        tv (get-version t)
        tpos? (get-positive? t)
        inc? (includes-version? tc tv v)]
    (if tpos?
      inc?
      (not inc?))))

;; This is a set operation since it works on ranges, not specific versions.
(defn satisfies?
  [terms term]
  nil)

;; This is a set operation since it works on ranges, not specific versions.
(defn contradicts?
  [terms term]
  nil)

(defn inconclusive?
  [terms term]
  (and (not (satisfies? terms term))
       (not (contradicts? terms term))))

(comment

  (parse "foo ^1.2.3")
  (includes? "foo 1.2.3" "1.2.3")
  (includes? "foo 1.2.3" "1.2.0")
  (includes? "foo ^1.2.3" "1.3.3")
  (includes? "foo ~1.2.3" "1.3.3")

  (includes? "foo ^1.3.0" "1.2.3")
  (includes? "foo ~1.3.0" "1.2.3")

  :rcf)
