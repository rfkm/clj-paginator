(ns clj-paginator.renderer.bootstrap3
  (:require [clj-paginator.renderer :refer :all]
            [clojure.string :as str]))

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

(defmethod render :bootstrap3 [pagination & opt]
  (let [intermediate (render-intermediate pagination)]
    `[:ul {:class "pagination"}
      ~@(map render-pager-element intermediate)]))
