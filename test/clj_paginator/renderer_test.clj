(ns clj-paginator.renderer-test
  (:require [clj-paginator.renderer :refer :all]
            [clj-paginator.core :refer [paginate]]
            [midje.sweet :refer :all]))

(facts "render-intermediate"
  (let [req {:uri "/"}]
    (fact "simple collection"
      (render-intermediate (paginate req (range 5) {:current-page 3 :limit 1}))
      => [[:prev 2 "/?page=2"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2"]
          [:page 3 "/?page=3" :active]
          [:page 4 "/?page=4"]
          [:page 5 "/?page=5"]
          [:next 4 "/?page=4"]])
    (fact "link attributes"
      (render-intermediate (paginate req (range 2) {:current-page 1 :limit 1}))
      => [[:prev nil nil]
          [:page 1 "/?page=1" :active]
          [:page 2 "/?page=2"]
          [:next 2 "/?page=2"]]
      (render-intermediate (paginate req (range 2) {:current-page 2 :limit 1}))
      => [[:prev 1 "/?page=1"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2" :active]
          [:next nil nil]])

    (fact "sliding"
      (render-intermediate (paginate req (range 5) {:current-page 1 :limit 1 :window-size 0}))
      => [[:prev nil nil]
          [:page 1 "/?page=1" :active]
          [:ellipsis]
          [:page 5 "/?page=5"]
          [:next 2 "/?page=2"]]

      (render-intermediate (paginate req (range 5) {:current-page 2 :limit 1 :window-size 0}))
      => [[:prev 1 "/?page=1"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2" :active]
          [:ellipsis]
          [:page 5 "/?page=5"]
          [:next 3 "/?page=3"]]

      (render-intermediate (paginate req (range 5) {:current-page 3 :limit 1 :window-size 0}))
      => [[:prev 2 "/?page=2"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2"]
          [:page 3 "/?page=3" :active]
          [:page 4 "/?page=4"]
          [:page 5 "/?page=5"]
          [:next 4 "/?page=4"]]

      (render-intermediate (paginate req (range 5) {:current-page 4 :limit 1 :window-size 0}))
      => [[:prev 3 "/?page=3"]
          [:page 1 "/?page=1"]
          [:ellipsis]
          [:page 4 "/?page=4" :active]
          [:page 5 "/?page=5"]
          [:next 5 "/?page=5"]]

      (render-intermediate (paginate req (range 5) {:current-page 5 :limit 1 :window-size 0}))
      => [[:prev 4 "/?page=4"]
          [:page 1 "/?page=1"]
          [:ellipsis]
          [:page 5 "/?page=5" :active]
          [:next nil nil]])))

(facts "renderer"
  (let [req {:uri "/"}]
    (facts "default renderer"
      (fact "simple collection"
        (render (paginate req (range 1 6) {:current-page 3 :limit 1}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=2"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li {:class "active"}
             [:a {:href "/?page=3"} "3"]]
            [:li nil [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=4"} "&raquo;"]]])
      (fact "link attributes"
        (render (paginate req (range 1 3) {:current-page 1 :limit 1}))
        => [:ul {:class "pagination"}
            [:li {:class "disabled"} [:span  "&laquo;"]]
            [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li nil [:a {:href "/?page=2"} "&raquo;"]]]
        (render (paginate req (range 1 3) {:current-page 2 :limit 1}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=1"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
            [:li {:class "disabled"} [:span "&raquo;"]]])

      (fact "sliding"
        (render (paginate req (range 1 6) {:current-page 1 :limit 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li {:class "disabled"} [:span  "&laquo;"]]
            [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "&hellip;"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=2"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:current-page 2 :limit 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=1"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
            [:li {:class "disabled"}
             [:span "&hellip;"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=3"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:current-page 3 :limit 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=2"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li {:class "active"} [:a {:href "/?page=3"} "3"]]
            [:li nil [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=4"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:current-page 4 :limit 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=3"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "&hellip;"]]
            [:li {:class "active"} [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=5"} "&raquo;"]]]

        (render (paginate req (range 1 6) {:current-page 5 :limit 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=4"} "&laquo;"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "&hellip;"]]
            [:li {:class "active"} [:a {:href "/?page=5"} "5"]]
            [:li {:class "disabled"} [:span "&raquo;"]]]))))
