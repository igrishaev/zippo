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


(defmacro as-node-pred
  {:style/indent 1}
  [[node] & body]
  `(fn [loc#]
     (let [~node (zip/node loc#)]
       ~@body)))


(defn loc-find [loc loc-pred]
  (->> loc
       (loc-seq)
       (filter loc-pred)
       (first)))


(defn loc-find-all [loc loc-pred]
  (->> loc
       (loc-seq)
       (filter loc-pred)))


(defn loc-update [loc loc-pred loc-fn & args]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (if (loc-pred loc)
        (recur (zip/next (apply loc-fn loc args)))
        (recur (zip/next loc))))))


(defn loc-update-all [loc loc-fn & args]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (recur (zip/next (apply loc-fn loc args))))))


(defn node-edit [loc loc-pred fn-node & args]
  (apply loc-update loc loc-pred zip/edit fn-node args))


(defn loc-children [loc]
  (when-let [loc-child (zip/down loc)]
    (->> loc-child
         (iterate zip/right)
         (take-while some?))))


(defn- -locs-children [locs]
  (mapcat loc-children locs))


(defn loc-layers [loc]
  (->> [loc]
       (iterate -locs-children)
       (take-while seq)))


(defn- -locs-seq-breadth [locs]
  (when (seq locs)
    (lazy-seq
     (concat locs
             (-locs-seq-breadth (-locs-children locs))))))


(defn loc-seq-breadth [loc]
  (-locs-seq-breadth [loc]))


(defn- -lookup-until [direction loc loc-pred]
  (->> loc
       (iterate direction)
       (take-while some?)
       (rest)
       (filter loc-pred)
       (first)))


(defn lookup-up [loc loc-pred]
  (-lookup-until zip/up loc loc-pred))


(defn lookup-left [loc loc-pred]
  (-lookup-until zip/left loc loc-pred))


(defn lookup-right [loc loc-pred]
  (-lookup-until zip/left loc loc-pred))


#_
(comment

  (def z
    (zip/vector-zip [1 [2] [2] [[3]]]))

  (zip/root (node-edit z (->loc-pred int?) + 3))

  (zip/root (node-edit z
                       (as-node-)


                       (->loc-pred int?) + 3))

  (zip/root (loc-update z (->loc-pred #(= 2 %)) zip/replace :foo))

  (zip/root (loc-update z (->loc-pred #(= 2 %)) zip/edit str "_aaa"))

  (zip/root (loc-update z (->loc-pred #(= 2 %)) zip/remove)))
