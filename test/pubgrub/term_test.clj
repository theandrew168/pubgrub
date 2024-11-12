(ns pubgrub.term-test
  (:require [clojure.test :as t]
            [pubgrub.term :as term]))

(t/deftest parse
  (t/testing "term/parse"
    (t/is (= (term/parse "foo 1.2.3") {:package "foo"
                                       :ranges [{:version "1.2.3"
                                                 :constraint :exact}]
                                       :positive? true}))
    (t/is (= (term/parse "foo ^1.2.3") {:package "foo"
                                        :ranges [{:version "1.2.3"
                                                  :constraint :major}]
                                        :positive? true}))))

(t/deftest includes?
  (t/testing "term/includes?"
    (t/is (term/includes? "foo 1.2.3" "1.2.3"))
    (t/is (term/includes? "foo ^1.0.0" "1.2.3"))
    (t/is (term/includes? "foo ^1.2.0" "1.2.3"))
    (t/is (not (term/includes? "foo ^1.3.0" "1.2.3")))
    (t/is (term/includes? "foo ~1.2.0" "1.2.3"))
    (t/is (not (term/includes? "foo ~1.3.0" "1.2.3")))
    (t/is (not (term/includes? "foo ~1.3.0" "0.2.3")))
    (t/is (term/includes? "foo >=1.2.3" "1.2.3"))
    (t/is (not (term/includes? "foo >1.2.3" "1.2.3")))
    (t/is (term/includes? "foo <=1.2.3" "1.2.3"))
    (t/is (not (term/includes? "foo <1.2.3" "1.2.3")))
    (t/is (term/includes? "foo 1.0.0 || 1.2.3" "1.2.3"))
    (t/is (term/includes? "foo ^1.0.0 || ^2.0.0" "2.3.4"))))
