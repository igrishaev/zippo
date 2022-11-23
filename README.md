# Zippo

Small additions to the standard `clojure.zip` package.

## Why

The `clojure.zip` package is a masterpiece yet misses some utility
functions. For example, finding locations, bulk updates, lookups, breadth-first
traversing and so on. This library brings some bits of missing functionality.

## Installation

Lein:

```clojure
[com.github.igrishaev/zippo "0.1.2"]
```

Deps.edn

```clojure
{com.github.igrishaev/zippo {:mvn/version "0.1.2"}}
```

## Usage & examples

First, import both Zippo and `clojure.zip`:

~~~clojure
(ns zippo.core-test
  (:require
   [clojure.zip :as zip]
   [zippo.core :as zippo]))
~~~

Declare a zipper:

~~~clojure
(def z
  (zip/vector-zip [1 [2 3] [[4]]]))
~~~

Now check out the following Zippo functions.

### A finite seq of locations

The `loc-seq` funtion takes a location and returns a lazy seq of locations
untill it reaches the end:

~~~clojure
(let [locs (zippo/loc-seq z)]
  (mapv zip/node locs))

;; get a vector of notes to reduce the output
[[1 [2 3] [[4]]]
 1
 [2 3]
 2
 3
 [[4]]
 [4]
 4]
~~~

This is quite useful to traverse a zipper without keeping in mind the ending
condition (`zip/end?`).

### Finding locations

The `loc-find` function looks for the first location that matches a predicate:

~~~clojure
(let [loc (zippo/loc-find
           z
           (fn [loc]
             (-> loc zip/node (= 3))))]

  (is (= 3 (zip/node loc))))
~~~

Above, we found a location which node equals 3.

The `loc-find-all` function finds all the locatins that match the predicate:

~~~clojure
(let [locs (zippo/loc-find-all
            z
            (zippo/->loc-pred (every-pred int? even?)))]

  (is (= [2 4]
         (mapv zip/node locs))))
~~~

Since the predicate accepts a location, you can check its children, siblings and
so on. For example, check if a location belongs to a special kind of parent.

However, most of the time you're interested in a value (node) rather than a
location. The `->loc-pred` function converts a node predicate, which accepts a
node, into a location predicate. In the example above, the line

~~~clojure
(zippo/->loc-pred (every-pred int? even?))
~~~

makes a location predicate which node is an even integer.

### Updating a zipper

Zippo offers some functions to update a zipper.

The `loc-update` one takes a location predicate, an update function and the rest
arguments. Here is how you douple all the even numbers in a nested vector:

~~~clojure
(let [loc
      (zippo/loc-update
       z
       (zippo/->loc-pred (every-pred int? even?))
       zip/edit * 2)]

  (is (= [1 [4 3] [[8]]]
         (zip/root loc))))
~~~

For the updating function, one may use `zip/append-child` to append a child,
`zip/remove` to drop the entire location and so on:

~~~clojure
(let [loc
      (zippo/loc-update
       z
       (fn [loc]
         (-> loc zip/node (= [2 3])))
       zip/append-child
       :A)]

  (is (= [1 [2 3 :A] [[4]]]
         (zip/root loc))))
~~~

The `node-update` function is similar but acts on nodes. Instead of `loc-pred`
and `loc-fn`, it accepts `node-pred` and `node-fn` what operate on nodes.

~~~clojure
(let [loc
    (zippo/node-update
     z
     int?
     inc)]
(is (= [2 [3 4] [[5]]]
       (zip/root loc))))
~~~

### Slicing a zipper by layers

Sometimes, you need to slice a zipper on layers. This is what is better seen on
a chart:

~~~
     +---ROOT---+    ;; layer 1
     |          |
   +-A-+      +-B-+  ;; layer 2
   | | |      | | |
   X Y Z      J H K  ;; layer 3
~~~

- Layer 1 is `[Root]`;
- Layer 1 is `[A B]`;
- Layer 3 is `[X Y Z J H K]`

The `loc-layers` function takes a location and builds a lazy seq of layers. The
first layer is the given location, then its children, the children of children
and so on.

~~~clojure
(let [layers
      (zippo/loc-layers z)]

  (is (= '(([1 [2 3] [[4]]])
           (1 [2 3] [[4]])
           (2 3 [4])
           (4))
         (for [layer layers]
           (for [loc layer]
             (zip/node loc))))))
~~~

### Breadth-first seq of locations

[depth-first]: https://en.wikipedia.org/wiki/Depth-first_search

The `clojure.zip` package uses [depth-first method][depth-first] of traversing a
tree. Let's number the items:

~~~
       +-----ROOT[1]----+
       |                |
 +----A[2]---+     +---B[6]--+
 |     |     |     |    |    |
 X[3] Y[4] Z[5]   J[7] H[8] K[9]
~~~

This sometimes may end up with an infinity loop when you generate children
on the fly.

The `loc-seq-breadth` functions offers the opposite way of traversing a zipper:

~~~
       +-----ROOT[1]----+
       |                |
 +----A[2]---+     +---B[3]--+
 |     |     |     |    |    |
 X[4] Y[5] Z[6]   J[7] H[8] K[9]
~~~

This is useful to solve some special tasks related to zippers.

### Lookups

When working with zippers, you often need such functionality as "go
up/left/right until meet something". For example, from a given location, go up
until a parent has a special attribute. Zippo offers four functions for that,
namely `lookup-up`, `lookup-left`, `lookup-right`, and `lookup-down.` All of
them take a location and a predicate:

~~~clojure
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
                               (= :b (first node))))))]

  (is (= :d (zip/node loc-d)))

  (is (= [:b [:c [:d]]] (zip/node loc-b))))
~~~

In the example above, first we find the `:d` location. From there, we go up
until we meet `[:b [:c [:d]]]`. If there is no such a location, the result will
be nil.

### A universal collection zipper

The `coll-zip` function builds a zipper that navigates through all the known
collections types, e.g. vectors, maps, map entries, lazy collections and so
on. Unlike the standard `zip/vector-zip` and `zip/seq-zip`, it works with any
combination of vectors and map which is quite useful in production. A brief
example:

```clojure
(def sample
  [{:foo 1}
   #{'foo 'bar 'hello}
   (list 1 2 3 {:aa [1 2 {:haha true}]})])

(->> sample
     coll-zip
     loc-seq
     (map zip/node))

(<initial data>
 {:foo 1}
 [:foo 1]
 :foo
 1
 #{bar hello foo}
 bar
 hello
 foo
 (1 2 3 {:aa [1 2 {:haha true}]})
 1
 2
 3
 {:aa [1 2 {:haha true}]}
 [:aa [1 2 {:haha true}]]
 :aa
 [1 2 {:haha true}]
 1
 2
 {:haha true}
 [:haha true]
 :haha
 true)
```

The `coll-zip` zipper carries a detailed implementation of the `make-node`
function. It takes into account the type of the node and properly builds a new
one from the children. It also preserves the metadata.

### Also See

[zippers-guide]: https://grishaev.me/en/clojure-zippers/

The code from this library was used for [Clojure Zippers manual][zippers-guide]
-- the complete guide to zippers in Clojure from the very scratch.

&copy; 2022 Ivan Grishaev
