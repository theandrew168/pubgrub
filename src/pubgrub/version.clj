(ns pubgrub.version
  (:require
   [clojure.string :as str]))

(defn parse
  "Parse a new version (sequence of integers)."
  [s]
  (map #(Integer/parseInt %) (str/split s #"\.")))

(defn extend-to
  "Extend a version out to N segments."
  [v n]
  (let [lv (count v)]
    (concat v (take (- n lv) (repeat 0)))))

(defn zip
  "Given two versions, extend them to match lengths and group into pairs."
  [a b]
  (let [n (max (count a) (count b))
        aa (extend-to a n)
        bb (extend-to b n)]
    (map vector aa bb)))

(defn equal?
  "Compare if two versions are equal."
  [a b]
  (let [z (zip a b)]
    (every? #(apply = %) z)))

(comment

  (parse "1.2.3")
  (extend-to (parse "1.2.3") 5)
  (extend-to (parse "1.2.3") 3)
  (zip (parse "1") (parse "1.2.3"))
  (equal? (parse "1") (parse "1.0.0"))
  (equal? (parse "1") (parse "1.0.1"))

  :rcf)
