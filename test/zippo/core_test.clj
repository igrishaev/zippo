(ns zippo.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.zip :as zip]
   [zippo.core :as zippo]))

(def z-vec
  [1 [2 3] [[4]]])

(def z
  (zip/vector-zip z-vec))


(deftest test-loc-seq

  (let [locs (zippo/loc-seq z)]

    (is (= 8 (count locs)))

    (is (= [z-vec
            1
            [2 3]
            2
            3
            [[4]]
            [4]
            4]

           (mapv zip/node locs)))))


(deftest test-loc-find

  (testing "found"

    (let [loc (zippo/loc-find
               z
               (fn [loc]
                 (-> loc zip/node (= 3))))]

      (is (= 3 (zip/node loc)))))

  (testing "not found"

    (let [loc (zippo/loc-find
               z
               (fn [loc]
                 (-> loc zip/node (= 99))))]

      (is (nil? loc))))

  (testing "loc pred"

    (let [loc (zippo/loc-find
               z
               (zippo/->loc-pred
                (fn [x]
                  (= x 4))))]

      (is (= 4 (zip/node loc))))))


(deftest test-loc-find-all

  (let [locs (zippo/loc-find-all
              z
              (zippo/->loc-pred (every-pred int? even?)))]

    (is (= [2 4]
           (mapv zip/node locs)))))


(deftest test-loc-update-simple

  (let [loc
        (zippo/loc-update
         z
         (zippo/->loc-pred (every-pred int? even?))
         zip/edit * 2)]

    (is (= [1 [4 3] [[8]]]
           (zip/root loc)))))


(deftest test-loc-update-append-child

  (let [loc
        (zippo/loc-update
         z
         (fn [loc]
           (-> loc zip/node (= [2 3])))
         zip/append-child
         :A)]

    (is (= [1 [2 3 :A] [[4]]]
           (zip/root loc)))))


(deftest test-loc-update-remove

  (let [loc
        (zippo/loc-update
         z
         (fn [loc]
           (-> loc zip/node (= [2 3])))
         zip/remove)]

    (is (= [1 [[4]]]
           (zip/root loc)))))


(deftest test-loc-update-all
  (let [loc
        (zippo/loc-update
         z
         (zippo/->loc-pred int?)
         zip/edit
         inc)]

    (is (= [2 [3 4] [[5]]]
           (zip/root loc)))))


(deftest test-node-update
  (let [loc
        (zippo/node-update
         z
         int?
         inc)]
    (is (= [2 [3 4] [[5]]]
           (zip/root loc)))))


(deftest test-loc-layers
  (let [layers
        (zippo/loc-layers z)]

    (is (= '(([1 [2 3] [[4]]])
             (1 [2 3] [[4]])
             (2 3 [4])
             (4))
           (for [layer layers]
             (for [loc layer]
               (zip/node loc)))))))
