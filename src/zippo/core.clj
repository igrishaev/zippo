(ns zippo.core
  (:require
   [clojure.zip :as zip]))


(defn loc-seq [loc]
  (->> loc
       (iterate zip/next)
       (take-while (complement zip/end?))))


(defn ->loc-pred [node-pred]
  (fn [loc]
    (-> loc zip/node node-pred)))


(defn find-all [loc loc-pred]
  (some (fn [-loc]
          (when (loc-pred -loc)
            -loc))
        (loc-seq loc)))


(defn find-first [loc loc-pred]
  (filter loc-pred (loc-seq loc)))


(defn update-where [loc loc-pred loc-fn & args]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (if (loc-pred loc)
        (recur (zip/next (apply loc-fn loc args)))
        (recur (zip/next loc))))))


(defn edit-where [loc loc-pred fn-node & args]
  (apply update-where loc loc-pred zip/edit fn-node args))


(defn- -locs-children [locs]
  (mapcat loc-children locs))


(defn loc-children [loc]
  (when-let [loc-child (zip/down loc)]
    (->> loc-child
         (iterate zip/right)
         (take-while some?))))


(defn loc-layers [loc]
  (->> [loc]
       (iterate -locs-children)
       (take-while seq)))


(defn- -locs-seq-breadth [locs]
  (when (seq locs)
    (lazy-seq (concat locs (foo-seq (-locs-children locs))))))


(defn loc-seq-breadth [loc]
  (-locs-seq-breadth [loc]))

#_
(comment

  (def z
    (zip/vector-zip [1 [2] [2] [[3]]]))

  (zip/root (edit-where z (->loc-pred int?) + 3))

  (zip/root (edit-where z (->loc-pred #(= 2 %)) zip/replace :foo))

  (zip/root (update-where z (->loc-pred #(= 2 %)) zip/replace :foo))

  (zip/root (update-where z (->loc-pred #(= 2 %)) zip/edit str "_aaa"))

  (zip/root (update-where z (->loc-pred #(= 2 %)) zip/remove))

  (-> z zip/up zip/up)



  (def -layers
    (-> z loc-layers))

  (def -seq (loc-seq-breadth z))

  (mapv zip/node -seq)

  [[1 [2] [2] [[3]]]

   1
   [2]
   [2]
   [[3]]
   2
   2
   [3]
   3]

  )
