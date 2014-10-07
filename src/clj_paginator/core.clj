(ns clj-paginator.core
  (:require [clj-paginator.utils :as u]
            [hiccup.core :refer [html]]
            [korma.core :refer :all]
            [potemkin :refer :all]))

;; (def-map-type LazyMap [m]
;;   (get [_ k default-value]
;;        (if (contains? m k)
;;          (let [v (get m k)]
;;            (if (instance? clojure.lang.Delay v)
;;              @v
;;              v))
;;          default-value))
;;   (assoc [_ k v]
;;     (LazyMap. (assoc m k v)))
;;   (dissoc [_ k]
;;           (LazyMap. (dissoc m k)))
;;   (keys [_]
;;         (keys m)))


;; (def-map-type LazyMap [m]
;;   (get [_ k default-value]
;;        (if (contains? m k)
;;          (let [v (get m k)]
;;            (if (instance? clojure.lang.Delay v)
;;              @v
;;              v))
;;          default-value))
;;   (assoc [_ k v]
;;     (LazyMap. (assoc m k v)))
;;   (dissoc [_ k]
;;           (LazyMap. (dissoc m k)))
;;   (keys [_]
;;         (keys m)))

;; (LazyMap )


;; (defn guess-target-type [target]
;;   (cond
;;    (u/korma? target) :korma
;;    (u/lazy? target)  :lazy
;;    (coll? target)    :collection))

;; (defn empty-pagination []
;;   {:page 1
;;    :limit 20
;;    :type :collection
;;    :target nil
;;    :window 0
;;    :outer-window 0
;;    :renderer nil
;;    ;; :count 0
;;    ;; :total-count nil
;;    ;; :previous nil
;;    ;; :next nil
;;    ;; :first-item-index nil
;;    ;; :last-item-index nil
;;    })



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

;; '(defn paginate [target page & [{:keys [type limit window]}]]
;;    (letfn [(assoc-1 [map key val]
;;              (if (nil? val)
;;                map
;;                (assoc map key val)))]
;;      (-> (empty-pagination)
;;          (assoc :type (or type
;;                           (guess-target-type target)))
;;          (assoc-1 :page page)
;;          (assoc-1 :limit limit)
;;          (assoc :target target))))


(defn paginate [target page & [{:keys [type limit window]}]]
  (let [total-count (count target)
        limit       (or limit 20)
        items       target
        num-items   (count items)
        start-idx   (if (pos? num-items) (inc (* (dec page) limit)) 0)
        end-idx     (if (pos? num-items) (dec (+ start-idx num-items)) 0)
        start-page  1
        max-page    (int (Math/ceil (/ total-count limit)))]
    {:total-count   total-count
     :items         items
     :count         num-items
     :start-index   start-idx
     :end-index     end-idx
     :current-page  page
     :start-page    start-page
     :pages         (for [i (range start-page (inc max-page))]
                      {:page i
                       :active? (= page i)})
     :max-page      max-page
     :next-page     (when (< page max-page) (inc page))
     :previous-page (when (> page 1) (dec page))}))

;; (defn paginate [target page & [{:keys [type limit window]}]]
;;   (let [items target]
;;     {:total-count total-count
;;      :limit (or limit 20)
;;      :items target                      ; FIX:
;;      :num-items (count )
;;      :next-page 4
;;      :previous-page 2
;;      :target target
;;      :pages [{:active? false :page "1"}
;;              {:active? false :page "2"}
;;              {:active? true  :page "3"}
;;              {:active? false :page "4"}
;;              {:active? false :page "5"}]}))

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


;; (def render nil)
;; (defmulti render (fn [target & _] (:renderer target)))

(defn get-next-page [pagination]
  (:next-page pagination))

(defn get-previous-page [pagination]
  (:previous-page pagination))

(defn has-next? [pagination]
  (not (nil? (get-next-page pagination))))

(defn has-previous? [pagination]
  (not (nil? (get-previous-page pagination))))

(defn get-pages [pagination]
  (:pages pagination))

(defn active? [page]
  (:active? page))

(defn render [pagination & opt]
  `[:ul {:class "pagination"}
    [:li ~(when-not (has-previous? pagination)
            {:class "disabled"})
     [:a {:href "#"} "&laquo;"]]

    ~@(for [page (get-pages pagination)]
        [:li (when (active? page)
               {:class "active"})
         [:a {:href "#"} (str (:page page))]])

    [:li ~(when-not (has-next? pagination)
            {:class "disabled"})
     [:a {:href "#"} "&raquo;"]]
    ])

;; (-> (paginate [1 2 3 4 5 6] 1)
;;     render
;;     html)

;; (html [:ul nil [:li "foo"]])


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
                  start-idx (if (pos? cnt) (inc (* (dec page) per-page)) 0)
                  end-idx (if (pos? cnt) (dec (+ start-idx cnt)) 0)
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
