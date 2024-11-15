(ns pubgrub.term
  (:require
   [clojure.string :as str]
   [pubgrub.version :as version]))

;; TODO: Should I write Normand-style getters and setters
;; for these fields?
(defn- make
  [package ranges positive?]
  {:package package
   :ranges ranges
   :positive? positive?})

(defn negate
  [t]
  (assoc t :positive? false))

;; Constraints can be one of:
;; :exact :major :minor :lt :lte :gt :gte
(defn- parse-range
  [s]
  (cond
    (str/starts-with? s "^") {:constraint :major :version (subs s 1)}
    (str/starts-with? s "~") {:constraint :minor :version (subs s 1)}
    (str/starts-with? s "<=") {:constraint :lte :version (subs s 2)}
    (str/starts-with? s ">=") {:constraint :gte :version (subs s 2)}
    (str/starts-with? s "<") {:constraint :lt :version (subs s 1)}
    (str/starts-with? s ">") {:constraint :gt :version (subs s 1)}
    :else {:constraint :exact :version s}))

(defn- split
  "Split a term on whitespace or pipe (for logical OR)."
  [s]
  (str/split s #"[\s|]+"))

(defn parse
  [s]
  (let [fields (split s)
        package (first fields)
        ranges (map parse-range (rest fields))]
    (make package ranges true)))

(defn- range-includes-version?
  [r v]
  (let [rc (r :constraint)
        rv (r :version)]
    (case rc
      :major (and (version/greater-than-or-equal? v rv)
                  (= (get v 0) (get rv 0)))
      :minor (and (version/greater-than-or-equal? v rv)
                  (= (get v 0) (get rv 0))
                  (= (get v 1) (get rv 1)))
      :lte (version/less-than-or-equal? v rv)
      :gte (version/greater-than-or-equal? v rv)
      :lt (version/less-than? v rv)
      :gt (version/greater-than? v rv)
      :exact (version/equal? v rv))))


(defn includes?
  "Check if a term includes a specific version."
  [t v]
  (some #(range-includes-version? % v) ((parse t) :ranges)))

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
  (parse "minipass ^5.0.0 || ^6.0.2 || ^7.0.0")
  (includes? "foo 1.2.3" "1.2.3")
  (includes? "foo 1.2.3" "1.2.0")
  (includes? "foo ^1.2.3" "1.3.3")
  (includes? "foo ~1.2.3" "1.3.3")

  (includes? "foo ^1.3.0" "1.2.3")
  (includes? "foo ~1.3.0" "1.2.3")

  :rcf)
