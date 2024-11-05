(ns pubgrub.core-test
  (:require [clojure.test :as t]
            [pubgrub.core :as pubgrub]))

(t/deftest parse-version
  (t/testing "parse-version"
    (t/is (= (pubgrub/parse-version "1") [1]))
    (t/is (= (pubgrub/parse-version "1.2") [1 2]))
    (t/is (= (pubgrub/parse-version "1.2.3") [1 2 3]))))

(t/deftest extend-version
  (t/testing "extend-version"
    (t/is (= (pubgrub/extend-version [1] 3) [1 0 0]))
    (t/is (= (pubgrub/extend-version [1 2 3] 3) [1 2 3]))))

(t/deftest zip-versions
  (t/testing "zip-versions"
    (t/is (= (pubgrub/zip-versions [1] [1]) [[1 1]]))
    (t/is (= (pubgrub/zip-versions [1 2 3] [1]) [[1 1] [2 0] [3 0]]))))

(t/deftest equal-versions?
  (t/testing "equal-versions?"
    (t/is (= (pubgrub/equal-versions? [1] [1 0 0]) true))
    (t/is (= (pubgrub/equal-versions? [1] [1 2 3]) false))))

(t/deftest parse-term
  (t/testing "parse-term"
    (t/is (= (pubgrub/parse-term "foo 1.2.3") {:package "foo"
                                               :version [1 2 3]
                                               :constraint :exact
                                               :negative? false}))
    (t/is (= (pubgrub/parse-term "not foo ^1.2.3") {:package "foo"
                                                    :version [1 2 3]
                                                    :constraint :major
                                                    :negative? true}))))
