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


(deftest test-loc-seq-breadth
  (let [locs
        (zippo/loc-seq-breadth z)]
    (is (= [[1 [2 3] [[4]]]
            1
            [2 3]
            [[4]]
            2
            3
            [4]
            4]
           (mapv zip/node locs)))))


(deftest test-lookup-up

  (let [loc
        (zip/vector-zip [:a [:b [:c [:d]]] :e])

        loc-d
        (zippo/loc-find loc
                        (zippo/->loc-pred
                         (fn [node]
                           (= node :d))))

        loc-b
        (zippo/lookup-up loc-d
                         (zippo/->loc-pred
                          (fn [node]
                            (and (vector? node)
                                 (= :b (first node))))))

        loc-not-found
        (zippo/lookup-up loc-d
                         (zippo/->loc-pred
                          (fn [node]
                            (= node 42))))]

    (is (= :d (zip/node loc-d)))

    (is (= [:b [:c [:d]]] (zip/node loc-b)))

    (is (nil? loc-not-found))))


(def sample
  [{:foo 1}
   #{'foo 'bar 'hello}
   (list 1 2 3 {:aa [1 2 {:haha true}]})])


(deftest test-coll-zip
  (let [loc
        (-> sample
            zippo/coll-zip
            (zippo/loc-find
             (fn [loc]
               (-> loc zip/node (= {:haha true})))))

        loc*
        (zip/edit loc assoc :extra 42)]

    (is (= (zip/root loc*)
           '[{:foo 1}
             #{bar hello foo}
             (1 2 3 {:aa [1 2 {:haha true
                               :extra 42}]})]))))


(deftest test-coll-build-node

  (is (= (zippo/coll-make-node [42] '(1 2 3 4))
         [1 2 3 4]))

  (is (= (zippo/coll-make-node #{42} '(1 2 3 4 3))
         #{1 2 3 4}))

  (is (= (zippo/coll-make-node
          {:some 'map}
          '((:key1 "a") ["key2" 2]))
         {:key1 "a" "key2" 2}))

  (let [entry
        (zippo/coll-make-node
         (-> {:some 'map} first)
         '(:new-key "new-val"))]

    (is #?(:clj (map-entry? entry)
           :cljs (vector? entry)))
    (is (= entry [:new-key "new-val"])))

  (is (= (zippo/coll-make-node
          (repeat 0 2)
          (repeat 3 2))
         '(2 2 2))))
