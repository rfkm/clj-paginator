(ns clj-paginator.renderer
  (:require [clojure.string :as str]
            ;; [hiccup.core :refer [html]]
            ))

(defn render-intermediate [pagination & opt]
  (let [pages                (:pages pagination)
        start-page-in-window (:page (first pages))
        end-page-in-window   (:page (last pages))
        end-page             (:end-page pagination)
        route                (:route-generator pagination)
        prev                 (:previous-page pagination)
        next                 (:next-page pagination)]
    (remove nil?
            `[~[:prev prev (when prev (route prev))]

              ~@(when (> start-page-in-window 1)
                  [[:page 1 (route 1)]
                   (cond
                    (= start-page-in-window 3)    [:page 2 (route 2)]
                    (not= start-page-in-window 2) [:ellipsis])])

              ~@(for [page pages
                      :let [p    (:page page)
                            link (route p)]]
                  (if (:active page)
                    [:page p link :active]
                    [:page p link]))

              ~@(when-not (= end-page end-page-in-window)
                  [(cond
                    (= end-page-in-window (- end-page 2))    [:page (dec end-page) (route (dec end-page))]
                    (not= end-page-in-window (dec end-page)) [:ellipsis])
                   [:page end-page (route end-page)]])

              ~[:next next (when next (route next))]])))



(defmulti render (fn [pagination & opt] (:renderer pagination)))
