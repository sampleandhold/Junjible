(ns cavapanel.server
  (:require [clojure.string :as str]
            [future-contrib.config :as config]
            [noir.server :as server]
            [noir.session :as session]
            [cavapanel.database :as db]
            [cavapanel.database-actions :as actions]
            [cavapanel.views.admin]
            [cavapanel.views.admindata]
            [cavapanel.views.common]
            [cavapanel.views.forgotpass]
            [cavapanel.views.panel]
            [cavapanel.views.panelserver]
            [cavapanel.views.register]
            [cavapanel.views.support]
            [cavapanel.views.tests]
            [cavapanel.views.welcome])
  (:use [future-contrib.java-utils]
        [cavapanel.utils]
        [redis.core :only [with-server]])
  (:import [java.util Calendar]
           [java.text SimpleDateFormat]))

;; NOTE: the following does NOT work in production (because we don't have src/)
;; (server/load-views "src/cavapanel/views/")
(server/load-views-ns 'cavapanel.views)

;; Noir in dev mode will show a stack trace instead of a 500 and
;; will aggressively try new routes

;; NOTE option precedence is
;;   *options-defaults*
;;   config file properties
;;   command line arguments

(def ^:dynamic #^{:doc "program name"}
  *program* "cavapanel")

;; defaults are ok here only if the key is in *options-ignore*
(def ^:dynamic #^{:doc "Command line options"}
  *options*
  [["-u" "--usage" "Show usage" :default false :flag true]
   ["-v" "--[no-]verbose" :default false]
   ["-c" "--conf" "Configuration file" :parse-fn config/file-must-exist]
   ["-h" "--host" "Host interface"]
   ["-p" "--port" (str "Run " *program*  " on this port") :parse-fn #(Integer. %)]
   ["-R" "--redis-host" "Connect to redis at this host"]
   ["-r" "--redis-port" "Connect to redis on this port" :parse-fn #(Integer. %)]
   ["-m" "--mode" "Noir mode (dev or prod)"]
   ["-n" "--database" "Database number" :parse-fn #(Integer. %)]
   ["-a" "--password" "Password"]
   ["-s" "--salt" "Global Salt"]
   ["-w" "--write-config" "Write config & exit" :flag true]
   ["-b" "--junjible-db" "Update junjible database & exit" :flag true]
   ])

(def ^:dynamic #^{:doc "Defaults in case nothing is specified on the command line or in the configuration file"}
  *options-defaults*
  {:host "0.0.0.0", :port 8080, 
   :redis-host "localhost", :redis-port 6379,
   :mode "prod",
   :conf (config/home-file (str "." *program*))})

(def ^:dynamic #^{:doc "Command line options to ignore in the properties file"}
  *options-ignore*
  ["usage" "verbose" "conf" "write-config" "junjible-db" "args"])

(defn run [opts]
  (let [password (:password opts)
        auth (if-let [pw password] (str "username:" pw "@") "")
        host (:host opts)
        port (as-int (:port opts))
        redis-host (:redis-host opts)
        redis-port (as-int (:redis-port opts))
        mode (:mode opts)
        n (as-int (:database opts))]
    (db/set-redisdb {:host redis-host
                     :port redis-port
                     :db n
                     :password password})
    (try
      (if (db/connect)
        (let [db-action (:junjible-db opts)]
          (db/initialize-cavapanel (not db-action)) ; don't verify on db-action
          (if db-action
            (do
              (actions/junjible-db (:args opts))
              (shutdown-agents) ;; important when sh is used
              )
            (do
              (verbose-msg "start noir in mode" mode)
              (server/start port {:mode mode
                                  :jetty-options { :host host }
                                  :ns 'cavapanel}))))
        (error-msg "unable to connect to redis"))
      (catch java.rmi.activation.ActivationException e ;; DEBUGGING
;;      (catch Exception e
        (error-msg *program* ": unable to connect to redis:" (.getMessage e))))))


(defn logRequest
  "Logs request to access.log and error.log"
  [handler]
  (fn [request]
    (let [resp (handler request)
          size (count (str resp))
          status (:status resp)
          okay-range [200, 301, 302, 304]
          okay? (= status (first
                            (filter
                              #(= status %)
                              okay-range)))
          remote-ip (:remote-addr request) 
          method (.toUpperCase 
                   (subs
                     (str
                       (:request-method request)) 1))
          headers (:headers request)
          user-agent (second
                       (first 
                         (filter 
                           #(= (first %) "user-agent")
                           headers)))
          uri (:uri request)
          referer (:referer request)
          dateFormat (new SimpleDateFormat "dd/MM/yyyy:hh:mm:ss +0000")
          current-time (.format dateFormat (.. Calendar
                                             (getInstance)
                                             (getTime)))
          log-row (str "[" current-time "] - " remote-ip " - "
                       "\"" method " " uri "\" "
                       status " " size " "
                       "\"" referer "\" "
                       "\"" user-agent "\"\n")
          ;;
          ;; Administrative Security Log
          ;;
          admin-info (with-server (db/redis-options)
                       (let [user (db/user?)
                             admin? (and user (db/admin-user? user))]
                         {:admin admin? :user-id user}))
          ]; let
      
      ; DEBUG
      ; (verbose-msg log-row "\nOkay?: " okay?)
      (try
        (if (= okay? false)
          ; Log Errors
          (spit "logs/error.log"
                log-row
                :append true)
          ; Log Access
          (spit "logs/access.log"
                log-row
                :append true))
        ;;
        ;; Administrative Security Log
        ;;
        (if (and (= true (:admin admin-info))
                 (= (:request-method request) :post))
          (let [username (:user-id admin-info)
                log-time current-time
                location uri
                method-type method
                ip remote-ip
                admin-log {:form-parms (:form-params request)
                           :params (:params request)
                           :url location
                           :method method-type}
                log-line (format "%s <%s [%s]> %s / Forms: %s / Params:%s "
                                 log-time
                                 username 
                                 ip
                                 (:url admin-log)
                                 (:form-params admin-log)
                                 (:params admin-log))]
            (spit "logs/admin-action.log"
                  (str log-line "\r\n")
                  :append true)))
        ;;      (catch java.rmi.activation.ActivationException e ;; DEBUG
      (catch Exception e
        (do (error-msg
              (.getMessage e))) ))
      resp)))

;; be sure to send any output to STDERR
(defn parse [argv options]
  (binding [*out* *err*]
    (config/parse argv options)))

;; be sure to send any output to STDERR
(defn read-config [program args options options-defaults options-ignore]
  (binding [*out* *err*]
    (config/read-config program args
                        options options-defaults
                        options-ignore)))

(defn -main [& argv]
  (try
    (let [[options args] (parse argv *options*)]
      (if (:usage options)
        (config/usage *program* *options* *options-defaults*)
        (let [opts (read-config *program* args
                                options *options-defaults*
                                *options-ignore*)]
          (if (:verbose opts)
              (set-verbose true))
          (verbose-msg "===== cavapanel =====")
          ;; DO NOT uncomment this, except for debugging as it will
          ;; expose sensitive information into log files
          ;; (verbose-msg (str "opts " opts))
          (if (:write-config opts)
            (config/write-config *program* opts *options-ignore*)
            ;; here the cond insures this function returns nil
            (do
              (server/add-middleware logRequest) ; Apache style logs
              (run opts)
              ;; (verbose-msg "run has returned")
              nil
              )))))
    (catch java.rmi.activation.ActivationException e ;; DEBUG
;;    (catch Exception e
      (do (error-msg (str *program* ":") (.getMessage e))
          (config/usage *program* *options* *options-defaults*)))))