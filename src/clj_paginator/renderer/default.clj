(ns clj-paginator.renderer.default
  (:require [clj-paginator.renderer :refer [render]]
            [clj-paginator.renderer.bootstrap3]))

(defmethod render :default [pagination & opt]
  (render (assoc pagination :renderer :bootstrap3)))
