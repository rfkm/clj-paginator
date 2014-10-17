(ns clj-paginator.core
  (:require [clj-paginator.utils :as u]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [korma.core :refer :all]
            [ring.util.codec :as codec]))


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


(defn get-pages-in-window [page max-page window]
  (let [st     (- page window)
        ed     (+ page window)
        offset (max 0 (- 1 st))
        st     (max 1 st)
        ed     (+ ed offset)
        offset (max 0 (- ed max-page))
        ed     (min max-page ed)
        st     (max 1 (- st offset))]
    (range st (inc ed))))

(defn- form-decode-map [params]
  (let [params (codec/form-decode (or params ""))]
    (if (map? params) params {})))

(defn gen-route [req page]
  (when page
    (let [query-string (-> req
                           :query-string
                           form-decode-map
                           (assoc "page" page)
                           codec/form-encode)]
      (str (:uri req) "?" query-string))))

(defn get-current-page-from-request [req]
  (try (-> req
           :query-string
           form-decode-map
           (get "page")
           (Integer/parseInt))
       (catch NumberFormatException e nil)))

(defn paginate [req target & [{:keys [page type limit window]}]]
  (let [page        (or page (get-current-page-from-request req) 1)
        total-count (count target)
        limit       (or limit 20)
        items       (vec target)
        num-items   (count items)
        start-idx   (if (pos? num-items) (inc (* (dec page) limit)) 0)
        end-idx     (if (pos? num-items) (dec (+ start-idx num-items)) 0)
        start-page  1
        max-page    (int (Math/ceil (/ total-count limit)))
        window      (or window 2)]
    {:page            page
     :total-count     total-count
     :items           items
     :count           num-items
     :start-index     start-idx
     :end-index       end-idx
     :current-page    page
     :start-page      start-page
     :pages           (for [i (get-pages-in-window page max-page window)]
                        {:page i
                         :active (= page i)})
     :max-page        max-page
     :next-page       (when (< page max-page) (inc page))
     :previous-page   (when (> page 1) (dec page))
     :route-generator (partial gen-route req)}))

(defn pager-element [content href & {:keys [disabled active]}]
  (let [cl (remove nil? [(when disabled "disabled")
                         (when active "active")])]
    [:li (when-not (empty? cl)
           {:class (str/join " " cl)})
     (if (and href (not disabled))
       [:a {:href href} (str content)]
       [:span  (str content)])]))

(defn hellip []
  (pager-element "&hellip;" nil :disabled true))

(defn render-intermediate [pagination & opt]
  (let [pages      (:pages pagination)
        start-page (:page (first pages))
        end-page   (:page (last pages))
        max-page   (:max-page pagination)
        route      (:route-generator pagination)
        prev       (:previous-page pagination)
        next       (:next-page pagination)]
    (remove nil?
            `[~[:prev prev (when prev (route prev))]

              ~@(when (> start-page 1)
                  [[:page 1 (route 1)]
                   (cond
                    (= start-page 3)    [:page 2 (route 2)]
                    (not= start-page 2) [:ellipsis])])

              ~@(for [page pages
                      :let [p    (:page page)
                            link (route p)]]
                  (if (:active page)
                    [:page p link :active]
                    [:page p link]))

              ~@(when-not (= max-page end-page)
                  [(cond
                    (= end-page (- max-page 2))    [:page (dec max-page) (route (dec max-page))]
                    (not= end-page (dec max-page)) [:ellipsis])
                   [:page max-page (route max-page)]])

              ~[:next next (when next (route next))]])))

(defmulti render-pager-element first)

(defmethod render-pager-element :prev [[_ _ link]]
  (pager-element "&laquo;" link :disabled (nil? link)))

(defmethod render-pager-element :next [[_ _ link]]
  (pager-element "&raquo;" link :disabled (nil? link)))

(defmethod render-pager-element :page [[_ page link & attrs]]
  (let [create-attr-pair (fn [attr] [attr (.contains (vec attrs) attr)]) ; e.g., => [:disabled true]
        possible-attrs [:active :disabled]]
    (->> possible-attrs
         (map create-attr-pair)
         flatten
         (apply pager-element page link))))

(defmethod render-pager-element :ellipsis [_]
  (hellip))

(defn render [pagination & opt]
  (let [intermediate (render-intermediate pagination)]
    `[:ul {:class "pagination"}
      ~@(map render-pager-element intermediate)]))


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
            (let [st     (- current-page (quot display-limit 2))
                  ed     (+ st (dec display-limit))
                  offset (max 0 (- 1 st))
                  st     (max 1 st)
                  ed     (+ ed offset)
                  offset (max 0 (- ed max-page))
                  ed     (min max-page ed)
                  st     (max 1 (- st offset))
                  ]
              (range st (inc ed))))

          (defn paginate [query page per-page]
            (let [whole-cnt (get-count query)
                  entities  (fetch-paginated-entities query page per-page)
                  cnt       (count entities)
                  start-idx (if (pos? cnt) (inc (* (dec page) per-page)) 0)
                  end-idx   (if (pos? cnt) (dec (+ start-idx cnt)) 0)
                  max-page  (int (Math/ceil (/ whole-cnt per-page)))]
              {:whole-count whole-cnt
               :entities entities
               :count cnt
               :start-index start-idx
               :end-index end-idx
               :current-page page
               :max-page max-page
               :pager-elements (for [i (gen-pager-elements page max-page 5)]
                                 {:page i
                                  :active (= page i)})
               :next? (< page max-page)
               :prev? (> page 1)
               :prev (dec page)
               :next (inc page)})))
