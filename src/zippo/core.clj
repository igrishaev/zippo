(ns zippo.core
  (:require
   [clojure.zip :as zip]))


(defn loc-seq
  "Get a lazy, finite seq of locations."
  [loc]
  (->> loc
       (iterate zip/next)
       (take-while (complement zip/end?))))


(defn ->loc-pred
  "Turn a node predicate into a location predicate."
  [node-pred]
  (fn [loc]
    (-> loc zip/node node-pred)))


(defn loc-find
  "Find the first location matches a predicate."
  [loc loc-pred]
  (->> loc
       (loc-seq)
       (filter loc-pred)
       (first)))


(defn loc-find-all
  "Find all the locations that match a predicate."
  [loc loc-pred]
  (->> loc
       (loc-seq)
       (filter loc-pred)))


(defn loc-update
  "Update locations that match the `loc-pred` function
  with the `loc-fn` functions and the rest arguments.
  Returns the last (end) location."
  [loc loc-pred loc-fn & args]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (if (loc-pred loc)
        (recur (zip/next (apply loc-fn loc args)))
        (recur (zip/next loc))))))


(defn loc-update-all
  "Update all the locations with the `loc-fn` and the rest
  arguments. Returns the last (end) location."
  [loc loc-fn & args]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (recur (zip/next (apply loc-fn loc args))))))


(defn node-update
  "Like `loc-update` but acts on nodes. Updates all the nodes
  that match `node-pred` with the `node-fn` function
  and the rest arguments."
  [loc node-pred node-fn & args]
  (apply loc-update
         loc
         (->loc-pred node-pred)
         zip/edit
         node-fn
         args))


(defn loc-children
  "Return all the children locations."
  [loc]
  (when-let [loc-child (zip/down loc)]
    (->> loc-child
         (iterate zip/right)
         (take-while some?))))


(defn locs-children
  "For a seq of locations, return their concatenated children."
  [locs]
  (mapcat loc-children locs))


(defn loc-layers
  "For a given location, return a lazy seq of its 'layers',
  e.g. children, the children of children and so on."
  [loc]
  (->> [loc]
       (iterate locs-children)
       (take-while seq)))


(defn- -locs-seq-breadth [locs]
  (when (seq locs)
    (lazy-seq
     (concat locs
             (-locs-seq-breadth (locs-children locs))))))


(defn loc-seq-breadth
  "Return a lazy seq of locations in breadth-first direction
  (left to right, down, left to right and so on)."
  [loc]
  (-locs-seq-breadth [loc]))


(defn- -lookup-until [direction loc loc-pred]
  (->> loc
       (iterate direction)
       (take-while some?)
       (rest)
       (filter loc-pred)
       (first)))


(defn lookup-up
  "Go up until a location matches a predicate."
  [loc loc-pred]
  (-lookup-until zip/up loc loc-pred))


(defn lookup-left
  "Go left until a location matches a predicate."
  [loc loc-pred]
  (-lookup-until zip/left loc loc-pred))


(defn lookup-right
  "Go right until a location matches a predicate."
  [loc loc-pred]
  (-lookup-until zip/left loc loc-pred))


(defn lookup-down
  "Go down until a location matches a predicate."
  [loc loc-pred]
  (-lookup-until zip/left loc loc-pred))


(defn coll-make-node
  [node children]
  (cond

    ;; MapEntry doesn't support meta
    (map-entry? node)
    (let [[k v] children]
      (new clojure.lang.MapEntry k v))

    :else
    (with-meta
      (cond

        (vector? node)
        (vec children)

        (set? node)
        (set children)

        (map? node)
        (persistent!
         (reduce ;; into {} doesn't work
          (fn [acc! [k v]]
            (assoc! acc! k v))
          (transient {})
          children))

        :else
        children)

      (meta node))))


(defn coll-zip
  "A zipper to navigate through any (nested) collection."
  [root]
  (zip/zipper coll?
              seq
              coll-make-node
              root))


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
