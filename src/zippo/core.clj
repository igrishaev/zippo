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


(defmacro as-loc-pred
  {:style/indent 1}
  [[node] & body]
  `(fn [loc#]
     (let [~node (zip/node loc#)]
       ~@body)))


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


(defn loc-children [loc]
  (when-let [loc-child (zip/down loc)]
    (->> loc-child
         (iterate zip/right)
         (take-while some?))))


(defn loc-layers [loc]
  (->> [loc]
       (iterate (fn [locs]
                  (mapcat
                   loc-children locs)))
       (take-while seq)))

(defn foo-seq [locs]
  (when (seq locs)
    (lazy-seq (concat locs (foo-seq (mapcat loc-children locs)))))

  #_
  (lazy-cat locs (foo-seq (mapcat loc-children locs)))
  #_
  (lazy-seq (concat locs (foo-seq (mapcat loc-children locs)))))


(defn aaa [n]
  (lazy-seq (cons n (aaa (inc n)))))


(defn foo-bar [loc]
  (foo-seq [loc]))

#_
(defn loc-layers [loc]

  (loop [result [loc]
         locs [loc]]

    (let [locs-next
          (mapcat loc-children locs)]

      (if (seq locs-next)
        (recur (into result locs-next) locs-next)
        result))))

#_
(comment

  (def z
    (zip/vector-zip [1 [2] [2] [[3]]]))

  (def -layers
    (-> z loc-layers))

  (def -seq (foo-bar (zip/vector-zip z)))

  (mapv zip/node -layers)

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
