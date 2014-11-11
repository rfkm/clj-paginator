(ns clj-paginator.adapter
  (:require [korma.core :as db]))

(defprotocol Pageable
  (count-all [this])
  (find-all [this page per-page]))

(extend-protocol Pageable
  clojure.lang.Sequential
  (count-all [this]
    (count this))
  (find-all [this page per-page]
    (if (pos? page)
      (take per-page (drop (* (dec page) per-page) this))
      (empty this))))

(defrecord KormaAdapter [query]
  Pageable
  (count-all [_]
    (-> query
        (db/aggregate (count :*) :cnt)
        db/select
        first
        :cnt))
  (find-all [this page per-page]
    (if (pos? page)
      (-> query
          (db/offset (* (dec page) per-page))
          (db/limit per-page)
          db/select)
      [])))
