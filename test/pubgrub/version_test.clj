(ns pubgrub.version-test
  (:require [clojure.test :as t]
            [pubgrub.version :as version]))

(t/deftest parse
  (t/testing "version/parse"
    (t/is (= (version/parse "1") [1]))
    (t/is (= (version/parse "1.2") [1 2]))
    (t/is (= (version/parse "1.2.3") [1 2 3]))))

(t/deftest equal?
  (t/testing "version/equal?"
    (t/is (version/equal? "1" "1.0.0"))
    (t/is (false? (version/equal? "1" "1.2.3")))))

(t/deftest less-than?
  (t/testing "version/less-than?"
    (t/is (not (version/less-than? "1.2.3" "1.2.3")))
    (t/is (not (version/less-than? "1.2.3" "1.2.2")))
    (t/is (version/less-than? "1.2.3" "1.2.4"))
    (t/is (version/less-than? "1.2.3" "2.0.0"))))

(t/deftest less-than-or-equal?
  (t/testing "version/less-than-or-equal?"
    (t/is (version/less-than-or-equal? "1.2.3" "1.2.3"))
    (t/is (not (version/less-than-or-equal? "1.2.3" "1.2.2")))
    (t/is (version/less-than-or-equal? "1.2.3" "1.2.4"))
    (t/is (version/less-than-or-equal? "1.2.3" "2.0.0"))))

(t/deftest greater-than?
  (t/testing "version/greater-than?"
    (t/is (not (version/greater-than? "1.2.3" "1.2.3")))
    (t/is (not (version/greater-than? "1.2.3" "1.2.4")))
    (t/is (version/greater-than? "1.2.3" "1.2.2"))
    (t/is (version/greater-than? "1.2.3" "0.0.0"))))

(t/deftest greater-than-or-equal?
  (t/testing "version/greater-than-or-equal?"
    (t/is (version/greater-than-or-equal? "1.2.3" "1.2.3"))
    (t/is (not (version/greater-than-or-equal? "1.2.3" "1.2.4")))
    (t/is (version/greater-than-or-equal? "1.2.3" "1.2.2"))
    (t/is (version/greater-than-or-equal? "1.2.3" "0.0.0"))))
