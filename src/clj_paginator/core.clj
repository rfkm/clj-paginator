(ns clj-paginator.core
  (:require [clj-paginator.adapter :refer :all]
            [clj-paginator.renderer.default]
            [ring.util.codec :as codec]))

(defn get-pages-in-window [page end-page window]
  (let [st     (- page window)
        ed     (+ page window)
        offset (max 0 (- 1 st))
        st     (max 1 st)
        ed     (+ ed offset)
        offset (max 0 (- ed end-page))
        ed     (min end-page ed)
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

(defn get-end-page [total-count limit]
  (int (Math/ceil (/ total-count limit))))

(defn paginate [req target & [{:keys [current-page type limit window-size]}]]
  (let [current-page (or current-page (get-current-page-from-request req) 1)
        total-count  (count-all target)
        limit        (or limit 20)
        items        (get-items target current-page limit)
        num-items    (count items)
        start-idx    (if (pos? num-items) (inc (* (dec current-page) limit)) 0)
        end-idx      (if (pos? num-items) (dec (+ start-idx num-items)) 0)
        start-page   1
        end-page     (get-end-page total-count limit)
        window-size       (or window-size 2)]
    {:current-page    current-page
     :total-count     total-count
     :items           items
     :count           num-items
     :start-index     start-idx
     :end-index       end-idx
     :start-page      start-page
     :pages           (for [i (get-pages-in-window current-page end-page window-size)]
                        {:page   i
                         :active (= current-page i)})
     :end-page        end-page
     :next-page       (when (< current-page end-page) (inc current-page))
     :previous-page   (when (> current-page 1) (dec current-page))
     :route-generator (partial gen-route req)}))
