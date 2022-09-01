# Zippo

Small additions to the standard `clojure.zip` package.

## Installation

Lein:

```clojure
[com.github.igrishaev/zippo "0.1.0"]
```

Deps.edn

```clojure
{com.github.igrishaev/zippo {:mvn/version "0.1.0"}}
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

### Updating a zipper

### Slicing a zipper by layers

### Breadth-first seq of locations

### Lookups

&copy; 2022 Ivan Grishaev
