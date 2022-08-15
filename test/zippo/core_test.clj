(ns zippo.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.zip :as zip]
   [zippo.core :as zippo]))


(def z
  (zip/vector-zip [1 [2 3] [[4]]]))


(deftest test-loc-seq

  (let [locs (zippo/loc-seq z)]

    (is (= 8 (count locs)))

    (is (= [[1 [2 3] [[4]]]
            1
            [2 3]
            2
            3
            [[4]]
            [4]
            4]

           (mapv zip/node locs)))))
