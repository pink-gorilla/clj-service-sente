(ns demo.page
  (:require
   [reagent.core :as r]
   [promesa.core :as p]
   [goldly.service.core :refer [clj]]))

(defn clj->atom [a & args]
  (println "executing clj: " args)
  (let [data-p (apply clj args)]
    (-> data-p
        (p/then (fn [result]
                  (reset! a result)))
        (p/catch (fn [err]
                   (println "error clj req: " args " error: " err))))
    nil))

(def quote-a (r/atom {}))
(def add-a (r/atom {}))
(def ex-a (r/atom {}))
(def slow-a (r/atom {}))
(def cookie-a (r/atom {}))

(defn clj-quote []
  ; clj exec no args
  (clj->atom quote-a 'demo.service/quote))

(defn clj-cookie []
  ; clj exec no args
  (clj->atom cookie-a 'demo.fortune-cookie/get-cookie))

(defn clj-add []
  ; clj exec with args
  (clj->atom add-a 'demo.service/add 2 7))

(defn clj-ex []
  ; test fn execution exception
  (clj->atom ex-a 'demo.service/ex))

(defn clj-quote-slow []
  (clj->atom slow-a {:timeout 1000} 'demo.service/quote-slow))

(defn show-page []
  [:div
   [:p.text-big.text-blue-900.text-bold "clj-services tests .."]

   [:div.bg-green-500.m-5.p-5
    [:button.bg-blue-500 {:on-click #(clj-quote)} "get quote (fast)"]
    [:p "result: " (pr-str @quote-a)]]

    [:div.bg-green-500.m-5.p-5
    [:button.bg-blue-500 {:on-click #(clj-cookie)} "stateful service - fortunedb"]
    [:p "result: " (pr-str @cookie-a)]]

   [:div.bg-green-500.m-5.p-5
    [:button.bg-blue-500 {:on-click #(clj-add)} "add numbers (clj)"]
    [:p "result: " (pr-str @add-a)]]

   [:div.bg-green-500.m-5.p-5
    [:button.bg-blue-500 {:on-click #(clj-quote-slow)} "get quote (slow)"]
    [:p "see log for timeout error logging."]
    [:p "result: " (pr-str @slow-a)]]])

(defn service-page [_route]
  [show-page])

