(ns goldly.service.core
  (:require
   [taoensso.timbre :refer-macros [info warn]]
   [promesa.core :as p]
   [reagent.core :as r]
   [modular.ws.core :refer [send!]]))

(defn print-result [[event-type data]]
  (warn "service result rcvd: type: " event-type " data: " event-type data))

; run with callback

(defn run-cb [{:keys [fun args timeout cb]
               :or {timeout 120000 ; 2 minute
                    cb print-result}
               :as params}]
  (let [p-clean (dissoc params :cb :a :where)]
    (info "running service " fun  " args: " fun args)
    (send! [:clj/service p-clean] cb timeout)
    nil))

(defn clj
  "executes clj function, returns a promise.
   first parameter is the fully qualified function symbol.
   second parameter is optionally a a map
    {:timeout milliseconds}
   all other parameter will be sent to the clj function."
  ([fun]
   (clj {} fun))
  ([fun & args]
   (let [[opts fun args] (if (map? fun)
                           [fun (first args) (rest args)]
                           [{} fun args])
         {:keys [timeout]
          :or {timeout 120000}} opts
         r (p/deferred)
         on-result (fn [msg]
                     (info "received clj result: " (:data msg))
                     (if (= msg :chsk/timeout)
                       (p/reject! r {:msg "timeout"})
                       (let [[_ data] msg
                             {:keys [result error]} data]
                         (if error
                           (p/reject! r error)
                           (p/resolve! r result)))))]
     (run-cb {:fun fun :args (into [] args) :timeout timeout :cb on-result})
     r)))

(defn clj-atom
  "executes clj function. same syntax as clj function.
   returns an atom that contains a map
   {:status (:pending :done :error)
    :data 
    :error}"
  [& args]
  (let [a (r/atom {:status :pending})
        rp (apply clj args)]
    (-> rp
        (p/then (fn [data] (swap! a assoc :status :done :data data)))
        (p/catch (fn [error] (swap! a assoc :status :error :error error))))
    a))