(ns demo.app
  (:require
   [webly.spa.env :refer [get-resource-path]]))

(defn wrap-webly [page match]
  [:div
   [page match]])

(def routes
  [["/" {:name 'demo.page/service-page}]
   ["/help" {:name 'demo.admin/help-page}]])

