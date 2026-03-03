(ns clj-service.core
  (:require
   [taoensso.timbre :as timbre :refer [info error]]
   [extension :refer [get-extensions]]
   [modular.writer :refer [write-edn-private]]
   [modular.permission.service :refer [add-permissioned-service]]
   [clj-service.websocket :refer [create-websocket-responder]]))

;; EXPOSE FUNCTION

(defn- resolve-symbol [s]
  (try
    (requiring-resolve s)
    (catch Exception ex
      (error "Exception in exposing service " s " - symbol cannot be required.")
      (throw ex))))

(defn expose-function
  "exposes one function 
   services args: this - created via clj-service.core
                  permission-service - created via modular.permission.core/start-permissions
   function args: service - fully qualified symbol
                  permission - a set following modular.permission role based access
                  fixed-args - fixed args to be passed to the function executor as the beginning arguments"
  [{:keys [services permission-service] :as this} {:keys [function permission fixed-args]
                                                   :or {fixed-args []
                                                        permission nil}}]
  (assert this "you need to pass the clj-service state")
  (assert permission-service "you need to pass the modular.permission.core state")
  (assert (symbol? function))
  (let [service-fn (resolve-symbol function)]
    (add-permissioned-service permission-service function permission)
    (swap! services assoc function {:service-fn service-fn
                                    :permission permission
                                    :fixed-args fixed-args})))

(defn expose-functions
  "exposes multiple functions with the same permission and fixed-args."
  [this
   {:keys [symbols permission fixed-args name]
    :or {permission nil
         fixed-args []
         name "services"}}]
  (assert (vector? symbols))
  (info "exposing [" name "]   permission: " permission " functions: " symbols)
  (doall
   (map (fn [function]
          (expose-function this {:function function
                                 :permission permission
                                 :fixed-args fixed-args})) symbols)))

; services list

(defn services-list [{:keys [services] :as _this}]
  (keys @services))

; start service

(defn- exts->services [exts k]
  (->> (get-extensions exts {k nil})
       (map k)
       (remove nil?)))

(defn expose-stateless-services-from-extensions [this exts]
  (let [services (exts->services exts :clj-services)]
    (write-edn-private :clj-services services)
    (doall
     (map #(expose-functions this %) services))))

;; stateful 

(defn ctx->fixed-args [this ctx]
  (cond 
    (keyword? ctx) (let [v (get (:env this) ctx)
                         ;v (if (var? v) (var-get v) v)
                         fixed-args [v]]
                     ;(info "keyword-fixed arg:" fixed-args)
                     fixed-args)
    (boolean? ctx) [(:env this)]
    :else nil
    ))

(defn expose-stateful-function
  [{:keys [env] :as this} {:keys [fun permission ctx]
                           :or {permission nil ctx nil}}]
  (info "exposing [" fun "]   permission: " permission " env " ctx)
  (expose-function this {:function fun
                         :permission permission
                         :fixed-args (ctx->fixed-args this ctx)}))

(defn expose-stateful-services-from-extensions [this exts]
  (let [funs (exts->services exts :clj-services2)
        all-funs (apply concat funs)]
    (write-edn-private :clj-services2 funs)
    (doall
     (map #(expose-stateful-function this %) all-funs))))

(defn env-clean [env]
  (->> env 
       (map (fn [[k v]]
              [k (if (var? v) (var-get v) v)] 
              ))
       (into {})))

(defn start-clj-services
  "starts the clj-service service.
   exposes stateless services that are discovered via the extension system.
   non stateless services need to be exposed via expose-service"
  [{:keys [exts env permission-service]}]
  (info "starting clj-services ..")
  (let [env (env-clean (or env {}))
        this {:services (atom {})
              :env env
              :permission-service permission-service}]
    ; expose services list (which is stateful)
    (expose-function this
                     {:function 'clj-service.core/services-list
                      :permission nil
                      :fixed-args [this]})
    ; expose services from extensions (which are stateless)
    (info "exposing stateless services from extension..")
    (expose-stateless-services-from-extensions this exts)
    (info "exposing stateful services from extension..")
    (expose-stateful-services-from-extensions this exts)
    (info "creating websocket responder..")
    ; create websocket message handler
    (create-websocket-responder this)
    (info "clj-services running!")
    ; return the service state
    this))

