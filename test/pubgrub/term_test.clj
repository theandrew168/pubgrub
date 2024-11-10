(ns pubgrub.term-test
  (:require [clojure.test :as t]
            [pubgrub.term :as term]))

(t/deftest parse-term
  (t/testing "parse-term"
    (t/is (= (term/parse "foo 1.2.3") {:package "foo"
                                       :version [1 2 3]
                                       :constraint :exact
                                       :negative? false}))
    (t/is (= (term/parse "not foo ^1.2.3") {:package "foo"
                                            :version [1 2 3]
                                            :constraint :major
                                            :negative? true}))))
