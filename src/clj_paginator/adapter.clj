(ns clj-paginator.adapter
  (:require [korma.core :as db]))

(defprotocol Pageable
  (get-total-count [this])
  (get-items [this page limit]))

(extend-protocol Pageable
  clojure.lang.Sequential
  (get-total-count [this]
    (count this))
  (get-items [this page limit]
    (if (pos? page)
      (take limit (drop (* (dec page) limit) this))
      (empty this))))

(defrecord KormaAdapter [query]
  Pageable
  (get-total-count [_]
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
