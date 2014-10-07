(ns clj-paginator.core
  (:require [clj-paginator.utils :as u]
            [hiccup.core :refer [html]]
            [korma.core :refer :all]))


(defn guess-target-type [target]
  (condr.
   (u/korma? target) :korma
   (u/lazy? target)  :lazy
   (coll? target)    :collection))

(defn empty-pagination []
  {:page 1
   :limit 20
   :type :collection
   :target nil
   :window 0
   :outer-window 0
   :renderer nil
   ;; :count 0
   ;; :total-count nil
   ;; :previous nil
   ;; :next nil
   ;; :first-item-index nil
   ;; :last-item-index nil
   })



;; (def get-total-count nil)
;; (defmulti get-total-count :type)

;; (defmethod get-total-count :lazy [pagination]
;;   nil)

;; (defmethod get-total-count :korma [pagination]
;;   (-> pagination
;;       (get :target)
;;       (aggregate (count :*) :cnt)
;;       select
;;       first
;;       :cnt))

;; (defmethod get-total-count :default [pagination]
;;   (-> pagination
;;       (get :target)
;;       count))

;; (def has-next? nil)
;; (defmulti has-next? :type)

;; (def has-previous? nil)
;; (defmulti has-previous? :type)

;; (defn paginate [target page & [{:keys [type limit window]}]]
;;   (letfn [(assoc-1 [map key val]
;;             (if (nil? val)
;;               map
;;               (assoc map key val)))]
;;     (-> (empty-pagination)
;;         (assoc :type (or type
;;                          (guess-target-type target)))
;;         (assoc-1 :page page)
;;         (assoc-1 :limit limit)
;;         (assoc :target target))))

;; (def get-next-page nil)
;; (defmulti get-next-page :type)
;; (defmethod get-next-page :default [pagination]
;;   (not (nil? (get-previous-page pagination))))

;; (def has-next? nil)
;; (defmulti has-next? :type)
;; (defmethod has-next? :default [pagination]
;;   (not (nil? (get-next-page pagination))))

;; (def has-previous? nil)
;; (defmulti has-previous? :type)
;; (defmethod has-previous? :default [pagination]
;;   (not (nil? (get-previous-page pagination))))


(def render nil)
(defmulti render (fn [target & _] (:renderer target)))

(defmethod render :default [pagination & opt]
  ;; (let [ul []])
  [:ul {:class "pagination"}
   [:li (when (not (has-next? pagination))
          {:class "disabled"})
    [:a {:href "#"} "&laquo;"]]

   (for [item (get-items pagination)]
     [:li [:a {:href "#"} "2"]])

   [:li (when (not (has-previous? pagination))
          {:class "disabled"})
    [:a {:href "#"} "&raquo;"]]

   [:li {:class "active"}
    [:a {:href "#"} "1"]]
   [:li [:a {:href "#"} "2"]]
   [:li [:a {:href "#"} "3"]]
   [:li [:a {:href "#"} "4"]]
   [:li [:a {:href "#"} "5"]]
   [:li [:a {:href "#"} "&raquo;"]]])

;; (-> (paginate [1 2 3 4 5 6] 1)
;;     render
;;     html)

(html [:ul nil [:li "foo"]])



(comment  (defn get-page [req]
            (util/->int (get-in req [:query-params "page"] "1")))

          (defn get-count [query]
            (-> query
                (aggregate (count :*) :cnt)
                select
                first
                :cnt))

          (defn fetch-paginated-entities [query page per-page]
            (-> query
                (offset (* (dec page) per-page))
                (limit per-page)
                select))

          (defn gen-pager-elements [current-page max-page display-limit]
            (let [st (- current-page (quot display-limit 2))
                  ed (+ st (dec display-limit))
                  offset (max 0 (- 1 st))
                  st (max 1 st)
                  ed (+ ed offset)
                  offset (max 0 (- ed max-page))
                  ed (min max-page ed)
                  st (max 1 (- st offset))
                  ]
              (range st (inc ed))))

          (defn paginate [query page per-page]
            (let [whole-cnt (get-count query)
                  entities (fetch-paginated-entities query page per-page)
                  cnt (count entities)
                  start-idx (if (> cnt 0) (inc (* (dec page) per-page)) 0)
                  end-idx (if (> cnt 0) (dec (+ start-idx cnt)) 0)
                  max-page (int (Math/ceil (/ whole-cnt per-page)))]
              {:whole-count whole-cnt
               :entities entities
               :count cnt
               :start-index start-idx
               :end-index end-idx
               :current-page page
               :max-page max-page
               :pager-elements (for [i (gen-pager-elements page max-page 5)]
                                 {:page i
                                  :active? (= page i)})
               :next? (< page max-page)
               :prev? (> page 1)
               :prev (dec page)
               :next (inc page)})))
