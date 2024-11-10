(ns pubgrub.version-test
  (:require [clojure.test :as t]
            [pubgrub.version :as version]))

(t/deftest parse-version
  (t/testing "parse-version"
    (t/is (= (version/parse "1") [1]))
    (t/is (= (version/parse "1.2") [1 2]))
    (t/is (= (version/parse "1.2.3") [1 2 3]))))

(t/deftest extend-version
  (t/testing "extend-version"
    (t/is (= (version/extend-to [1] 3) [1 0 0]))
    (t/is (= (version/extend-to [1 2 3] 3) [1 2 3]))))

(t/deftest zip-versions
  (t/testing "zip-versions"
    (t/is (= (version/zip [1] [1]) [[1 1]]))
    (t/is (= (version/zip [1 2 3] [1]) [[1 1] [2 0] [3 0]]))))

(t/deftest equal-versions?
  (t/testing "equal-versions?"
    (t/is (= (version/equal? [1] [1 0 0]) true))
    (t/is (= (version/equal? [1] [1 2 3]) false))))
