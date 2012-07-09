(ns cavapanel.views.stats
  (:require [clojure.string :as str]
            [noir.response :as response]
            [hiccup.page-helpers :as web]
            [clj-json.core :as json]
	    [cavapanel.vars :as vars]
	    [cavapanel.views.common :as common]
	    [cavapanel.views.panelserver :as panelserver]
            [cavapanel.database :as db])
  (:use [noir.core]
        [clojure.set]
        [future-contrib.java-utils :only [as-int]]
        [hiccup.core]
        [redis.core :only [with-server]]
        [cavapanel.utils]))

(defpage [:post "/stats/update-instance"] {:as statsData}
  (with-server (db/redis-options)
    ;; Debug
    ;; (println "\n\n====\n\n" statsData)
    ;; (let [stats (keywordize (:stats statsData))
    (let [stats1 (:stats statsData)
          stats (if (map? stats1) stats1 (keywordize (json/parse-string stats1)))
          server (:server stats)
          host (:host stats)
          port (:port stats)
          players (as-int (:players stats))
          timestamp (as-long (:timestamp stats))
          token (:token stats)
          result {:success true :msg "ok"}]
      ;; NOTE: may use players < 0 to indicate server failure per #133
      (verbose-msg "/stats/update-instance: statsData=" statsData)
      (verbose-msg " stats=" stats)
      (verbose-msg " server" server
                   "host" host
                   "port" port
                   "players" players
                   "timestamp" timestamp
                   "token" token)
      (response/json (panelserver/debug-response result)))))
