(ns clj-service.websocket
  (:require
   [clojure.string]
   [de.otto.nom.core :as nom]
   [taoensso.timbre :as timbre :refer [warn error]]
   [modular.ws.msg-handler :refer [-event-msg-handler send-response]]
   [clj-service.executor :refer [execute-with-binding]]))

(defn error-response [user fun args r uid]
  (error "clj-service execution had an error: " r)
  (let [response {:error "Execution exception"
                  :user uid
                  :fun fun
                  :args args}]
    (error "sending error-response: " response)))

(defn response [r]
  {:result r})

(defn create-websocket-responder [this]
  (defmethod -event-msg-handler :clj/service
    [{:keys [event _id _?data uid client-id uid] :as req}]
    (warn "websocket browser-client-id: " client-id " uid:" uid)
    ; client-id is browser-session id
    ; uid is our username
    (let [[_ params] event ; _ is :clj/service
          {:keys [fun args]} params]
      (future
        (let [r (execute-with-binding this uid client-id fun args)]
          (if (nom/anomaly? r)
            (send-response req :clj/service (error-response fun args r uid))
            (send-response req :clj/service (response r))))))))
