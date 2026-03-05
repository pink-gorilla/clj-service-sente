(ns clj-service.sente
  (:require
   [clojure.string]
   [taoensso.timbre :as timbre :refer [warn error]]
   [clj-service.executor :refer [execute]]
   [clj-service.response :refer [response-success response-error]]
   [modular.ws.msg-handler :refer [-event-msg-handler send-response]]))


(defn create-websocket-responder [ctx]
  ; ctx needs to have :clj :token
  (defmethod -event-msg-handler :clj/service
    [{:keys [event _id _?data uid client-id uid] :as req}]
    (warn "websocket browser-client-id: " client-id " uid:" uid)
    ; client-id is browser-session id
    ; uid is our username
    (let [[_ clj-call] event ; _ is :clj/service
          setup {:session client-id :user uid}]
      (future
        (try 
          (let [r (execute ctx setup clj-call)]
            (send-response req :clj/service (response-success r)))
          (catch Exception ex
            (let [msg (or (ex-message ex) "server-error")
                  data (or (ex-data ex) {})]
              (send-response req :clj/service (response-error msg data)))))))))



