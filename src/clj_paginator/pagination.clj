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

(defn total-entries [pagination]
  @(get pagination :total-entries))

(defn entries [pagination]
  @(get pagination :entries))

(defn window-size [pagination]
  (get pagination :window-size))

(defn total-pages* [pagination]
  (int (Math/ceil (/ (total-entries pagination) (per-page pagination)))))

(defn total-pages [pagination]
  @(get pagination :total-pages))

(defn next-page [pagination]
  (let [page (page pagination)]
    (when (< page (total-pages pagination)) (inc page))))

(defn previous-page [pagination]
  (let [page (page pagination)]
    (when (> page 1) (dec page))))

(defn pages-in-window
  ([pagination]
     (pages-in-window (page pagination)
                      (total-pages pagination)
                      (window-size pagination)))
  ([page last-page window-size]
     (let [st     (- page window-size)
           ed     (+ page window-size)
           offset (max 0 (- 1 st))
           st     (max 1 st)
           ed     (+ ed offset)
           offset (max 0 (- ed last-page))
           ed     (min last-page ed)
           st     (max 1 (- st offset))]
       (range st (inc ed)))))

;; (defn paginate [req pageable & [{:keys [page per-page window-size]}]]
;;   (let [page        (or page (get-current-page-from-request req) 1)
;;         per-page    (or per-page 20)
;;         total-entries (count-all pageable)
;;         entries       (find-all pageable page per-page)
;;         window-size (or window-size 2)]
;;     {:page            page
;;      :window-size     window-size
;;      :per-page        per-page
;;      :total-entries   total-entries
;;      :entries         entries
;;      :route-generator (partial gen-route req)}))

(defn paginate [req pageable & [{:keys [page per-page window-size]}]]
  (let [page        (or page (get-current-page-from-request req) 1)
        per-page    (or per-page 20)
        window-size (or window-size 2)
        entries     (find-all pageable page per-page)
        pagination  {:page            page
                     :window-size     window-size
                     :per-page        per-page
                     :total-entries   (future (count-all pageable))
                     :entries         (future (find-all pageable page per-page))
                     :route-generator (partial gen-route req)}
        pagination  (assoc pagination :total-pages (future (total-pages* pagination)))]
    pagination))
