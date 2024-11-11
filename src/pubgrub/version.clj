(ns pubgrub.version
  (:require
   [clojure.string :as str]))

(defn parse
  "Parse a new version (sequence of integers)."
  [s]
  (map #(Integer/parseInt %) (str/split s #"\.")))

(defn- extend-to
  "Extend a version out to N segments."
  [v n]
  (let [lv (count v)]
    (concat v (take (- n lv) (repeat 0)))))

(defn- zip
  "Given two versions, extend them to match lengths and group into pairs."
  [a b]
  (let [a (parse a)
        b (parse b)
        n (max (count a) (count b))
        aa (extend-to a n)
        bb (extend-to b n)]
    (map vector aa bb)))

(defn equal?
  "Compare if two versions are equal."
  [a b]
  (let [z (zip a b)]
    (every? #(apply = %) z)))

;; TODO: Refactor these ineqs to use shared recursive logic.

(defn less-than?
  [a b]
  (loop [z (zip a b)]
    (if (empty? z)
      false
      (let [zz (first z)
            aa (get zz 0)
            bb (get zz 1)]
        (cond
          (< aa bb) true
          (> aa bb) false
          :else (recur (rest z)))))))

(defn less-than-or-equal?
  [a b]
  (loop [z (zip a b)]
    (if (empty? z)
      true
      (let [zz (first z)
            aa (get zz 0)
            bb (get zz 1)]
        (cond
          (< aa bb) true
          (> aa bb) false
          :else (recur (rest z)))))))

(defn greater-than?
  [a b]
  (loop [z (zip a b)]
    (if (empty? z)
      false
      (let [zz (first z)
            aa (get zz 0)
            bb (get zz 1)]
        (cond
          (< aa bb) false
          (> aa bb) true
          :else (recur (rest z)))))))

(defn greater-than-or-equal?
  [a b]
  (loop [z (zip a b)]
    (if (empty? z)
      true
      (let [zz (first z)
            aa (get zz 0)
            bb (get zz 1)]
        (cond
          (< aa bb) false
          (> aa bb) true
          :else (recur (rest z)))))))

(comment

  (parse "1.2.3")
  (extend-to (parse "1.2.3") 5)
  (extend-to (parse "1.2.3") 3)
  (zip (parse "1") (parse "1.2.3"))

  (equal? "1" "1.0.0")
  (equal? "1" "1.0.1")

  (less-than? "1.2.3" "1.2.3")
  (less-than? "1.2.3" "1.2.4")
  (less-than? "1.2.3" "2.0.0")
  (less-than? "1.2.3" "1.2.2")

  :rcf)
