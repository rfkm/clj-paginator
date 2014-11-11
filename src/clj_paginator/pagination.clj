(ns clj-paginator.pagination
  (:require [clj-paginator.adapter :refer :all]
            [ring.util.codec :as codec]))

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

(defn page [pagination]
  (get pagination :page))

(defn per-page [pagination]
  (get pagination :per-page))

(defn total-items [pagination]
  (get pagination :total-items))

(defn window-size [pagination]
  (get pagination :window-size))

(defn total-pages [pagination]
  (int (Math/ceil (/ (total-items pagination) (per-page pagination)))))

(defn next-page [pagination]
  (let [page (page pagination)]
    (when (< page (total-pages pagination)) (inc page))))

(defn previous-page [pagination]
  (let [page (page pagination)]
    (when (> page 1) (dec page))))

(defn pages-in-window* [page last-page window-size]
  (let [st     (- page window-size)
        ed     (+ page window-size)
        offset (max 0 (- 1 st))
        st     (max 1 st)
        ed     (+ ed offset)
        offset (max 0 (- ed last-page))
        ed     (min last-page ed)
        st     (max 1 (- st offset))]
    (range st (inc ed))))

(defn pages-in-window [pagination]
  (let [page        (page pagination)
        last-page   (total-pages pagination)
        window-size (window-size pagination)]
    (pages-in-window* page last-page window-size)))

(defn paginate [req pageable & [{:keys [page per-page window-size]}]]
  (let [page        (or page (get-current-page-from-request req) 1)
        per-page    (or per-page 20)
        total-items (count-all pageable)
        items       (find-all pageable page per-page)
        window-size (or window-size 2)]
    {:page            page
     :window-size     window-size
     :per-page        per-page
     :total-items     total-items
     :items           items
     :route-generator (partial gen-route req)}))
