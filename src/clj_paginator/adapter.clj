(ns clj-paginator.adapter
  (:require [korma.core :as db]))

(defprotocol Pageable
  (count-all [this])
  (get-items [this page limit]))

(extend-protocol Pageable
  clojure.lang.Sequential
  (count-all [this]
    (count this))
  (get-items [this page limit]
    (if (pos? page)
      (take limit (drop (* (dec page) limit) this))
      (empty this))))

(defrecord KormaAdapter [query]
  Pageable
  (count-all [_]
    (-> query
        (db/aggregate (count :*) :cnt)
        db/select
        first
        :cnt))
  (get-items [this page limit]
    (if (pos? page)
      (-> query
          (db/offset (* (dec page) limit))
          (db/limit limit)
          db/select)
      [])))
