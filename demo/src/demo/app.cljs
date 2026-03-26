(ns demo.app
  (:require
   [shadowx.core :refer [get-resource-path]]))

(defn wrap-webly [page match]
  [:div
   [page match]])

(def routes
  [["/" {:name 'demo.page/service-page}]
   ["/help" {:name 'demo.admin/help-page}]])

