(ns clj-paginator.core-test
  (:require [midje.sweet :refer :all]
            [clj-paginator.core :refer :all]
            [clj-paginator.utils :as u]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [clojure.java.jdbc :as sql]))


;; (defdb mem-db (h2 {:db "mem:test"}))

;; (defn truncate [table]
;;   (exec-raw [(str "TRUNCATE " table)]))

;; (exec-raw "CREATE TABLE user (
;;   id int(11) NOT NULL,
;;   name varchar(255) DEFAULT NULL,
;;   PRIMARY KEY (\"id\")
;; )")



;; (defentity user
;;   (table "USER"))

;; (select user)

;; (facts "guess-target-type"
;;   (fact "collection"
;;     (guess-target-type [0 1 2]) => :collection)
;;   (fact "lazy seq"
;;     (guess-target-type (range 10)) => :lazy)
;;   (fact "Korma"
;;     (guess-target-type (select* :user)) => :korma))

;; (facts "paginate"
;;   (fact "should have valid schema."
;;     (paginate [1 2 3] 1) => (contains {:type :collection
;;                                        :page integer?
;;                                        :limit integer?
;;                                        ;; :total-count (some-checker integer? nil?)
;;                                        :target [1 2 3]
;;                                        ;; :count integer?
;;                                        :window integer?
;;                                        :renderer anything
;;                                        ;; :outer-window integer?
;;                                        ;; :has-next? anything
;;                                        ;; :has-previous?? anything
;;                                        ;; :previous (some-checker integer? nil?)
;;                                        ;; :next (some-checker integer? nil?)
;;                                        ;; :first-item-index (some-checker integer? nil?)
;;                                        ;; :last-item-index (some-checker integer? nil?)
;;                                        }))
;;   (fact "custom target type"
;;     (paginate [] 1 {:limit 1 :type :my-type}) => (contains {:type :my-type})))

;; (facts "get total count"
;;   (fact ":collection"
;;     (get-total-count (paginate [1 2 3] 1)) => 3)
;;   (fact ":lazy return nil"
;;     (get-total-count (paginate (range 1) 1)) => nil))

(tabular
 (fact "gen-route"
   (gen-route {:uri "/users" :query-string ?query} ?page) => ?ret)
 ?query           ?page ?ret
 ""               3     "/users?page=3"
 "page=2"         3     "/users?page=3"
 "foo=bar&page=1" 3     (some-checker "/users?page=3&foo=bar" "/users?foo=bar&page=3"))

(facts "get-pages-in-window"
  (tabular
   (fact "center"
     (get-pages-in-window ?page 10 1) => ?ret)
   ?page ?ret
   5     [4 5 6]
   1     [1 2 3]
   2     [1 2 3]
   3     [2 3 4]
   4     [3 4 5]
   9     [8 9 10]
   10    [8 9 10]))

(facts "paginate"
  (fact "can get current page from query string"
    (paginate {:uri "/" :query-string "page=3"} (range 10)) => (contains {:page 3}))
  (fact "can specify current page via options map"
    (paginate {} (range 10) {:page 2}) => (contains {:page 2})
    (paginate {:uri "/" :query-string "page=3"} (range 10) {:page 2}) => (contains {:page 2})))

(facts "renderer"
  (let [req {:uri "/"}]
    (facts "default renderer"
      (fact "simple collection"
        (render (paginate req (range 1 6) {:page 3 :limit 1})) => [:ul {:class "pagination"}
                                                                   [:li nil [:a {:href "/?page=2"} "&laquo;"]]
                                                                   [:li nil [:a {:href "/?page=1"} "1"]]
                                                                   [:li nil [:a {:href "/?page=2"} "2"]]
                                                                   [:li {:class "active"}
                                                                    [:a {:href "/?page=3"} "3"]]
                                                                   [:li nil [:a {:href "/?page=4"} "4"]]
                                                                   [:li nil [:a {:href "/?page=5"} "5"]]
                                                                   [:li nil [:a {:href "/?page=4"} "&raquo;"]]])
      (fact "link attributes"
        (render (paginate req (range 1 3) {:page 1 :limit 1})) => [:ul {:class "pagination"}
                                                                   [:li {:class "disabled"} [:span  "&laquo;"]]
                                                                   [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
                                                                   [:li nil [:a {:href "/?page=2"} "2"]]
                                                                   [:li nil [:a {:href "/?page=2"} "&raquo;"]]]
        (render (paginate req (range 1 3) {:page 2 :limit 1})) => [:ul {:class "pagination"}
                                                                   [:li nil [:a {:href "/?page=1"} "&laquo;"]]
                                                                   [:li nil [:a {:href "/?page=1"} "1"]]
                                                                   [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
                                                                   [:li {:class "disabled"} [:span "&raquo;"]]])

      (fact "sliding"
        (render (paginate req (range 1 6) {:page 1 :limit 1 :window 0})) => [:ul {:class "pagination"}
                                                                             [:li {:class "disabled"} [:span  "&laquo;"]]
                                                                             [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
                                                                             [:li {:class "disabled"}
                                                                              [:span "&hellip;"]]
                                                                             [:li nil [:a {:href "/?page=5"} "5"]]
                                                                             [:li nil [:a {:href "/?page=2"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:page 2 :limit 1 :window 0})) => [:ul {:class "pagination"}
                                                                             [:li nil [:a {:href "/?page=1"} "&laquo;"]]
                                                                             [:li nil [:a {:href "/?page=1"} "1"]]
                                                                             [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
                                                                             [:li {:class "disabled"}
                                                                              [:span "&hellip;"]]
                                                                             [:li nil [:a {:href "/?page=5"} "5"]]
                                                                             [:li nil [:a {:href "/?page=3"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:page 3 :limit 1 :window 0})) => [:ul {:class "pagination"}
                                                                             [:li nil [:a {:href "/?page=2"} "&laquo;"]]
                                                                             [:li nil [:a {:href "/?page=1"} "1"]]
                                                                             [:li nil [:a {:href "/?page=2"} "2"]]
                                                                             [:li {:class "active"} [:a {:href "/?page=3"} "3"]]
                                                                             [:li nil [:a {:href "/?page=4"} "4"]]
                                                                             [:li nil [:a {:href "/?page=5"} "5"]]
                                                                             [:li nil [:a {:href "/?page=4"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:page 4 :limit 1 :window 0})) => [:ul {:class "pagination"}
                                                                             [:li nil [:a {:href "/?page=3"} "&laquo;"]]
                                                                             [:li nil [:a {:href "/?page=1"} "1"]]
                                                                             [:li {:class "disabled"}
                                                                              [:span "&hellip;"]]
                                                                             [:li {:class "active"} [:a {:href "/?page=4"} "4"]]
                                                                             [:li nil [:a {:href "/?page=5"} "5"]]
                                                                             [:li nil [:a {:href "/?page=5"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:page 5 :limit 1 :window 0})) => [:ul {:class "pagination"}
                                                                             [:li nil [:a {:href "/?page=4"} "&laquo;"]]
                                                                             [:li nil [:a {:href "/?page=1"} "1"]]
                                                                             [:li {:class "disabled"}
                                                                              [:span "&hellip;"]]
                                                                             [:li {:class "active"} [:a {:href "/?page=5"} "5"]]
                                                                             [:li {:class "disabled"} [:span "&raquo;"]]]))))
