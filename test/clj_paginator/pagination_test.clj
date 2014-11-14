(ns clj-paginator.pagination-test
  (:require [midje.sweet :refer :all]
            [clj-paginator.pagination :refer :all]
            [clj-paginator.utils :as u]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [clojure.java.jdbc :as sql]))

(tabular
 (fact "gen-route"
   (gen-route {:uri "/users" :query-string ?query} ?page) => ?ret)
 ?query           ?page ?ret
 ""               3     "/users?page=3"
 "page=2"         3     "/users?page=3"
 "foo=bar&page=1" 3     (some-checker "/users?page=3&foo=bar" "/users?foo=bar&page=3"))

(facts "pages-in-window"
  (tabular
   (fact "center"
     (pages-in-window ?page 10 1) => ?ret)
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
