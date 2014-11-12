(ns clj-paginator.renderer
  (:require [clojure.string :as str]
            [clj-paginator.pagination :refer :all]))

(defn render-intermediate [pagination & opt]
  (let [current              (page pagination)
        pages                (pages-in-window pagination)
        start-page-in-window (first pages)
        last-page-in-window  (last pages)
        last-page            (total-pages pagination)
        route                (:route-generator pagination)
        prev                 (previous-page pagination)
        next                 (next-page pagination)]
    (remove nil?
            `[~[:prev prev (when prev (route prev))]

              ~@(when (> start-page-in-window 1)
                  [[:page 1 (route 1)]
                   (cond
                    (= start-page-in-window 3)    [:page 2 (route 2)]
                    (not= start-page-in-window 2) [:ellipsis])])

              ~@(for [page pages
                      :let [link (route page)]]
                  (if (= page current)
                    [:page page link :active]
                    [:page page link]))

              ~@(when-not (= last-page last-page-in-window)
                  [(cond
                    (= last-page-in-window (- last-page 2))    [:page (dec last-page) (route (dec last-page))]
                    (not= last-page-in-window (dec last-page)) [:ellipsis])
                   [:page last-page (route last-page)]])

              ~[:next next (when next (route next))]])))

(defmulti render (fn [pagination & opt] (:renderer pagination)))
