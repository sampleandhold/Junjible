(ns cavapanel.vars)

(def ^:dynamic #^{:doc "Verbosity (boolean)"}
  *verbose* (atom false))

(def ^:dynamic #^{:doc "Current Redis database object"}
  *redisdb* (atom nil))

(def ^:dynamic #^{:doc "Last time (System/nanoTime) that Redis was consulted"}
  *redis-last* (atom 0))

(def ^:dynamic #^{:doc "map from redis type to operation verbs"}
  *redis-ops* (atom nil))

;; map from redis key to redis type 
(def ^:dynamic
  *redis-types*
  {"allowed" :set,
   "users" :set,
   "user" :hash,
   "groups" :hash,
   "group" :set,
   "worlds" :set,
   "world" :hash,
   "instances" :set,
   "permissions" :hash,
   "categories" :set,
   "plugins" :set,
   "plugin_perms" :set,
   "plugin_deps" :set,
   "user_perms" :set,
   "group_perms" :set,
   "servers" :set,
   "server" :hash, })

(def ^:dynamic #^{:doc "special registrants that inherit admin priv"}
  *admin-users*
  #{"raison@gmail.com", "testuser@email.com", "quinn@nematte.com",
    "theinternet@yandex.com", "mail.voidware@gmail.com",
    "quinn.finney@gmail.com", "tmarble@info9.net",
    "raison@chatsubo.net", "jake@nematte.com", "finn777@gmail.com"})

(def ^:dynamic #^{:doc "initial instance map"}
  *instance-default-settings*
  {:server ""
   :n 0
   :host ""
   :port 0
   :maxheap "1024m"
   :world "My World"
   :state "stopped",
   :msg ""
   :changes false
   :updated 0})

(def ^:dynamic #^{:doc "non settings keywords in plugin_settings:PLUGIN"}
  *plugin-settings-keywords*
  #{:updated :name :category :available :featured :desc
    :thumburl :screenshoturl :id})

;; NOTE: the case of rename=OLDWORLD must be handled separately
(def ^:dynamic #^{:doc "valid server junjible-instance actions"}
  *server-actions*
  #{"configure" "start" "stop"
    "status" "restart" "delete"})

(def ^:dynamic #^{:doc "work in progress result message"}
  *result-msg* (atom ""))

;; BUG FIXME: this is *not* setup to be unique per thread, but
;; globally unique
(def ^:dynamic #^{:doc "set of new elemnts (groups, worlds, instances)"}
  *save-groups* (atom nil))

(def ^:dynamic #^{:doc "set of new elemnts (groups, worlds, instances)"}
  *save-worlds* (atom nil))

(def ^:dynamic #^{:doc "set of new elemnts (groups, instances, instances)"}
  *save-instances* (atom nil))

(def ^:dynamic #^{:doc "pathname on server nodes to the start script"}
  *junjible-script* "./Junjible-Server/bin/junjible-instance")

(def ^:dynamic #^{:doc "time this program started"}
  *start* (atom 0))

(def ^:dynamic #^{:doc "Junjible server min port for minecraft servers (default)"}
  *junjible-min-port* 2000)

(def ^:dynamic #^{:doc "Junjible server max port for minecraft servers (default)"}
  *junjible-max-port* 32766)

(def ^:dynamic #^{:doc "success value"}
  *success* (atom true))

(def ^:dynamic #^{:doc "refresh interval for caching instance state (ms)"}
  *instance-update-interval* 60000)

(def ^:dynamic #^{:doc "time interval for completing most instance actions (ms)"}
  *instance-update-epsilon* 5000)

(def ^:dynamic #^{:doc "time after stop to undeploy world (ms)"}
  *undeploy-after-stop* 15000)

