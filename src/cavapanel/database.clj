(ns cavapanel.database
  (:import [java.net URLEncoder])
  (:require [clojure.string :as str]
            ;; REDIS [clj-redis.client :as redis]
            [redis.core :as redis]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [future-contrib.java-utils :as utils]
            [password-storage.core :as pw]
            [clj-time.core :as time]
            [clj-time.coerce :as time-convert]
            [clj-time.format :as time-format]
            [cavapanel.vars :as vars])
  (:use [clojure.java.shell]
        [clojure.set]
        [cavapanel.utils]))

(defn now-long []
  (time-convert/to-long (time/now)))

;; REDIS specific ===================================================

(defn set-redisdb [newredisdb]
  ;; (println "set-redisdb BEFORE:" (redisdb))
  (swap! vars/*redisdb* use-new newredisdb)
  ;; (println "set-redisdb AFTER:" (redisdb))
  )

;; REDIS
;; (defn db []
;;   (:pool @vars/*redisdb*))
;; NOW this means the database number


(defn redis-options []
  @vars/*redisdb*)

;; NOTE these two functions can probably go away as we select the
;; Database number when we startup
(defn db []
  (:db (redis-options)))

(defn select-db []
  (redis/select (db)))

(defn connect []
  (let [r @vars/*redisdb*]
    ;; REDIS (if-let [ndb (:db r)] ndb 0)]
    ;; REDIS  rdb (redis/init r)]
    (redis/with-server r
      ;; REDIS (if (= (redis/ping rdb) "PONG")
      (if (= (redis/ping) "PONG")
        (do
          ;; REDIS (set-redisdb (merge r {:pool rdb}))
          ;; REDIS (redis/select (db) n)
          (verbose-msg "connected to redis database:" (db))
          ;; REDIS (swap! vars/*redis-last* use-new (System/nanoTime))
          true)
        (do
          (error-msg "ERROR connect: unable to connect to redis database:" (db))
          false)))))

;; will reconnect to redis if necessary. FFI see
;; https://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.html
;; src/main/java/redis/clients/jedis/JedisPool.java
;;
;; REDIS
;; (defn reconnect []
;;   (let [lastcall @vars/*redis-last*
;;         maxIdleTime (* 60 1E9) ; 60 x 10^9 nanoseconds
;;         thiscall (System/nanoTime)]
;;     (if (compare-and-set! vars/*redis-last* lastcall thiscall)
;;       (if (> (- thiscall lastcall) maxIdleTime)
;;         (do
;;           (flush)
;;           (print "reconnect: won lock, reconnecting...")
;;           (connect)
;;           (flush))
;;         ;; (do
;;         ;;   (flush)
;;         ;;   (println "reconnect: request" thiscall)
;;         ;;   (flush))
;;         )
;;       (do ;; did NOT win the lock!
;;         (flush)
;;         (println "reconnect: did not win the lock, backing off...")
;;         (flush)
;;         (Thread/sleep (int (* 1000 (rand)))) ;; random backoff up to 1ms
;;         (reconnect)))))

;; cavapanel interface to redis --------------------------------------------

(defn ping []
  ;; REDIS (reconnect)
  ;; REDIS (redis/ping (db)))
  (redis/ping))

;; (defmulti del
;;   "delete a key or keys"
;;   class)

;; (defmethod del String [#^String key]
;;   (println"string del:" key)
;;   (del (list key)))

;; (defmethod del :default [keys]
;;   (println "list del:" keys)
;;   (redis/del keys))

(defn del [key]
  (verbose-msg "simple del:" key)
  (redis/del key))

(defn rget [key]
  ;; REDIS (reconnect)
  ;; REDIS(redis/get (db) key))
  (redis/get key))

(defn rset [key val]
  ;; REDIS (reconnect)
  ;; REDIS(redis/set (db) key val))
  (redis/set key val))

(defn rkeys [ & [pattern]]
  ;; REDIS (reconnect)
  ;; REDIS(redis/keys (db) (or pattern "*")))
  (redis/keys (or pattern "*")))

(defn hgetall [key]
  ;; REDIS (reconnect)
  ;; REDIS(java2hash (redis/hgetall (db) key)))
  (java2hash (redis/hgetall key)))

(defn hget [key field]
  ;; REDIS (reconnect)
  ;; REDIS(redis/hget (db) key (name field)))
  (redis/hget key (name field)))

(defn hdel [key field]
  ;; REDIS (reconnect)
  ;; (verbose-msg "hdel key=" key "=, field=" field "=")
  ;; REDIS(redis/hdel (db) key (name field)))
  (redis/hdel key (name field)))

(defn hmset [key fvmap]
  ;; REDIS (reconnect)
  ;; REDIS(redis/hmset (db) key (unkeywordize fvmap)))
  ;; DOES NOT WORK (redis/hmset key (unkeywordize fvmap)))
  (apply redis/hmset
         (cons key
               (apply merge-lists
                      (for [k (keys fvmap)]
                        (list (name k) (get fvmap k)))))))

;; insure field is NOT stored as a keyword
(defn hset [key field val]
  ;; REDIS (reconnect)
  ;; REDIS(redis/hset (db) key (name field) val))
  (redis/hset key (name field) val))

(defmulti sadd 
  "Add one or more members to a set"
  (fn [key members] (class members)))

(defmethod sadd String [key member]
  ;; REDIS (reconnect)
  ;;(println "sadd String key:" key "member:" member)
  ;; REDIS(redis/sadd (db) key member))
  (redis/sadd key member))

(defmethod sadd :default [key members]
  ;; REDIS (reconnect)
  ;;(println "sadd seq key:" key "members:" members)
  ;; REDIS(doseq [m members] (redis/sadd (db) key m)))
  (doseq [m members] (redis/sadd key m)))

(defn smembers [key]
  ;; REDIS (reconnect)
  ;; REDIS(set (redis/smembers (db) key)))
  (set (redis/smembers key)))

(defn srem [key member]
  ;; REDIS (reconnect)
  ;; REDIS (redis/srem (db) key member))
  (redis/srem key member))

(defn sismember [key member]
  ;; (verbose-msg "db/sismember key=" key "=, member=" member "=")
  ;; REDIS (reconnect)
  ;; REDIS(redis/sismember (db) key member))
  (redis/sismember key member))

;; all types delete
(defn adel [key]
  (let [kbase (first (str/split key #":"))
        ktype (get vars/*redis-types* kbase)
        kdel (:del (get @vars/*redis-ops* ktype))]
    (kdel key)
    ;; (verbose-msg "deleted key:" key)
    ))

(defn valid-key? [key]
  (if (nil? (re-matches #".*[:;].*" key))
    true
    false))

;; globals  --------------------------------------------------------------

(defn get-global [k]
  (mixed? (hget "globals" k)))

(defn is-global? [k]
  (let [v (get-global k)
        b (is-true? v)]
    (verbose-msg "is-global?" k "=" b)
    b
    ))

(defn get-globals []
  (let [gmap (hgetall "globals")]
    (if gmap
      (mixed-map gmap))))

(defn set-global [k v]
  (hset "globals" k (mixed-str? v)))

(defn get-servers []
  (smembers "servers"))

(defn set-instance-changes [server changes]
  (verbose-msg "set-instance-changes for server:" server)
  (let [sikeys (rkeys (str "server_instance:" server ":*"))]
    (doseq [sikey sikeys]
      (let [sival (rget sikey)]
        (if (and (not-empty sival) (not= sival "reserved"))
          (let [userinst (if sival (str/split sival #"[:]"))
                user (first userinst)
                instance (nth userinst 1 nil)
                instkey (if instance (str "instance:" user ":" instance))
                oldchanges (if instkey (is-true? (hget instkey "changes")))]
            (if (not= oldchanges changes)
              (do
                (verbose-msg (str "setting changes for \"" sikey
                                  "\" user \"" user
                                  "\" instance \"" instance
                                  "\" to " changes))
                (hset instkey "changes" (is-true-str? changes))))))))))

;; lockout all actions on servers
;; optional :msg "why they are locked"
(defn servers-locked [& opts_]
  (let [opts (keywordize (apply hash-map opts_))
        msg (if-let [m (:msg opts)] m "updating...")]
    (set-global "servers-locked" msg)
    (str "servers locked: " msg)))

;; unlock all actions on servers AND notify ALL instances of pending changes
(defn servers-unlocked []
  (let [servers (get-servers)]
    (doseq [server servers]
      (set-instance-changes server true)))
  (set-global "servers-locked" false)
  "servers unlocked")

(defn servers-locked? []
  (get-global "servers-locked"))

;; users --------------------------------------------------------------

;; forward looking definitions
(defn update-usergroup [])
(defn update-userworld [])
(defn update-userinstance [])
(defn update-serverchanged [])

;; NOTE: the sort order is important when we are paging sets of users
(defn get-users []
  (apply sorted-set (smembers "users")))

(defn valid-user? [user]
  (if user
    (sismember "users" user)
    false))

;; (println "DEBUG valid-user? user=" user
;;          "class of r=" (.getClass r)
;;          "r=" r)

(defn not-user? [user]
  (not (valid-user? user)))

(defn boolean-default [k m d]
  (let [v (mixed-boolean? (get m k))]
    ;; (println "DEBUG boolean-default k=" k
    ;;          "\n m=" m
    ;;          "\n d=" d
    ;;          "\n v=" v)
    (let [r (if (and
                 (not (nil? v))
                 (= (.getClass v) Boolean))
              v
              d)]
      ;; (println "DEBUG boolean-default r=" r
      ;;          "\n (= (.getClass v) Boolean))" (= (.getClass v) Boolean))
      r)))

;; lazy initialization -- return updated usermap
(defn initialize-user [user]
  (let [userkey (str "user:" user)
        usermap (mixed-map (hgetall userkey))
        reg (if-let [r (:registered usermap)] r 0)
        registered (if (> reg 0) reg (now-long))
        ;; FIXME passwords stored in clear text
        pass  (if-let [p (:pass usermap)] p "password")
        email  (if-let [e (:email usermap)] e "notset")
        ;; default minecraft to user name
        minecraft  (if-let [m (:minecraft usermap)] m user)
        specialuser (contains? vars/*admin-users* email)
        admin (boolean-default :admin usermap specialuser)
        active (boolean-default :active usermap true)
        level  (if-let [l (:level usermap)] l "unpaid")
        usage  (if-let [u (:usage usermap)] u "basic")
        usermap1 (hash-map :registered registered
                           :email email
                           :pass pass
                           :minecraft minecraft
                           :admin admin
                           :active active
                           :level level
                           :usage usage)]
    (hmset userkey (mixed-str-map usermap1))
    ;; add default group with the user called "Default"
    (update-usergroup user "Default" user)
    ;; add default world
    (update-userworld user "My World"
                      {:motd (str "Welcome to " user " world")
                       :online "true"
                       :private "true"})
    ;; add default instance
    (update-userinstance user "My Server" vars/*instance-default-settings*)
    (verbose-msg "initialized user" user)
    usermap1))

;; CAUTION this will return ALL fields for the user
(defn get-user [user]
  ;; (println "DEBUG get-user:" user)
  (if (not-user? user)
    (error-msg "get-user: user does not exist:" user)
    (let [userkey (str "user:" user)
          usermap (mixed-map (hgetall userkey))
          registered (if-let [r (:registered usermap)] r 0)]
      ;; (println "DEBUG get-user: usermap=" usermap)
      (let [um (if (= registered 0)
                 (initialize-user user)
                 usermap)]
        ;; (println "DEBUG get-user returning usermap=" um)
        um))))

(defn admin-user? [user]
  (let [usermap (if user (get-user user))
        admin (if (:admin usermap) true false)]
    admin))

(defn add-user [user & [usermap]]
  (if (not (valid-key? user))
    (str "error: add-user: invalid user key:" user)
    (if (valid-user? user)
      (str "error: add-user: user already exists:" user)
      (let [userkey (str "user:" user)
            unregistered (hash-map :registered 0)
            usermap1 (if usermap
                       (merge usermap unregistered)
                       unregistered)]
        ;; (println "DEBUG add-user: user=" user
        ;;          " usermap1=" usermap1)
        (sadd "users" user)
        ;; (println "DEBUG hmset userkey=" userkey)
        (hmset userkey (mixed-str-map usermap1))
        ;; (println "DEBUG initialize user...")
        (initialize-user user)
        (str "added user " user)))))

(defn set-user [user usermap]
  (if (not-user? user)
    (str "error: set-user: user does not exist " user)
    (let [userkey (str "user:" user)]
      ;; (println "DEBUG set-user: user=" user
      ;;          " usermap=" usermap)
      (hmset userkey (mixed-str-map usermap))
      (str "updated user " user))))

;; CAUTION does NOT verify password before updating
(defn update-user [user usermap & [verbose-changes]]
  ;; (println "DEBUG update-user: user=" user
  ;;          " usermap=" usermap)
  (let [oldusermap (get-user user)]
    ;; (println "DEBUG update-user: oldusermap=" oldusermap)
    (if (not oldusermap)
      (add-user user usermap)
      (if (= usermap oldusermap)
        (if-str verbose-changes "no change for the user " user)
        (set-user user usermap)))))

;;  REMOVE users USER -> any key maching *:USER$ or *:USER:
(defn del-user [user]
  (let [re (re-pattern (str ".*:" user "(:.*)?$"))]
    (doseq [k (sort (rkeys))]
      (if (first (re-matches re k))
        (adel k)))
    (srem "users" user))
  (str "deleted user " user))

;; groups -------------------------------------------------------------------

(defn add-usergroup [user group & [gdesc]]
  (let [desc (or gdesc "my new group")]
    (if (not (valid-key? group))
      (str "add-usergroup: invalid group key:" group)
      (if (not (sismember "users" user))
        (str "add-usergroup: user does NOT exist:" user)
        (let [groupskey (str "groups:" user)
              gdesc (hget groupskey group)]
          (if gdesc
            (str "add-usergroup: group already exists:" user ":" group)
            (do
              (hset groupskey group desc)
              (str "added group " group))))))))
;;              (str "added group " group "=" desc))))))))

(defn del-usergroup [user group]
  (if (not (valid-key? group))
    (str "del-usergroup: invalid group key:" group)
    (if (not (sismember "users" user))
      (str "del-usergroup: user does NOT exist:" user)
      (let [groupskey (str "groups:" user)
            gdesc (hget groupskey group)
            groupkey (str "group:" user ":" group)
            gmembers (smembers groupkey)]
        (if (nil? gdesc)
          (str "del-usergroup: group does not exist:" user ":" group)
          (do
            (if (not (empty? gmembers))
              (del groupkey))
            (hdel groupskey group)
            (str "deleted group " group)))))))
;;            (str "deleted group: " user ":" group)))))))

(defn get-usergroup [user group]
  (if (not (valid-key? group))
    (error-msg "get-usergroup: invalid group key:" group)
    (if (not (sismember "users" user))
      (error-msg "get-usergroup: user does NOT exist:" user)
      (let [groupskey (str "groups:" user)
            gdesc (hget groupskey group)
            groupkey (str "group:" user ":" group)
            gmembers (smembers groupkey)]
        (if (nil? gdesc)
          nil
          ;; (error-msg "get-usergroup: group does not exist:" user ":" group)
          (apply sorted-set gmembers))))))

(defn set-usergroup [user group members]
  (if (not (valid-key? group))
    (str "set-usergroup: invalid group key:" group)
    (if (not (sismember "users" user))
      (str "set-usergroup: user does NOT exist:" user)
      (let [groupskey (str "groups:" user)
            gdesc (hget groupskey group)
            groupkey (str "group:" user ":" group)
            gmembers (smembers groupkey)]
        (if (nil? gdesc)
          (str "set-usergroup: group does not exist:" user ":" group)
          (do
            (if (not (empty? gmembers))
              (del groupkey)) ;; "deleted old members of group:" groupkey
            (sadd groupkey members)
            (str "updated group " group " membership")))))))
;;            (str "set group " group " membership to: " (str-addcomma members))))))))

(defn update-usergroup [user group members & [verbose-changes]]
  (let [groupskey (str "groups:" user)
        gdesc (hget groupskey group)
        gmembers (get-usergroup user group)
        addmsg (if (nil? gdesc) (add-usergroup user group))]
    (if (= members gmembers)
      (if (not (nil? verbose-changes))
        (str "no change for the group " group))
      (let [setmsg (set-usergroup user group members)]
        (str-addline (if addmsg addmsg setmsg))))))

;; worlds ---------------------------

(defn get-userworld [user world]
  (if (not-user? user)
    (error-msg "get-userworld: user does not exist:" user)
    (let [worldskey (str "worlds:" user)
          world? (sismember worldskey world)
          worldkey (if world? (str "world:" user ":" world))
          worldmap (if worldkey (hgetall worldkey))]
      ;; (verbose-msg "get-userworld user=" user
      ;;              "\n world=" world
      ;;              "\n worldmap=" worldmap)
      worldmap)))

(defn get-userworlds-set [user]
  (if (not-user? user)
    (error-msg "get-userworlds-set: user does not exist:" user)
    (let [worldskey (str "worlds:" user)
          worlds (apply sorted-set (smembers worldskey))]
      worlds)))

(defn get-userworlds-safe [user]
  (let [worlds (get-userworlds-set user)]
    (if worlds
      (for [world worlds] (URLEncoder/encode world)))))

(defn get-userworlds [user & [addnames]]
  (if (not-user? user)
    (error-msg "get-userworlds: user does not exist:" user)
    (let [worlds (get-userworlds-set user)]
      ;; (verbose-msg "  get-userworlds = " worlds)
      (if (not (empty? worlds))
        (for [world worlds]
          (let [worldmap (get-userworld user world)]
            ;; (verbose-msg "  get-userworlds world = " world)
            (if addnames
              (merge worldmap (hash-map :name world))
              worldmap)))))))

(defn add-userworld [user world & [ worldmap ]]
  ;; (verbose-msg "add-userworld user=" user
  ;;              "\n world=" world
  ;;              "\n worldmap=" worldmap)
  (if (not-user? user)
    (str "error: add-userworld: user does not exist:" user)
    (if (not (valid-key? world))
      (str "add-userworld: invalid world key:" world)
      ;; if we have a non empty worldmap here, we can assume that
      ;; update-userworld is calling us -- so we know that this world
      ;; does not exist.
      (if (and (empty? worldmap) (get-userworld user world))
        (str "error: add-userworld: world already exists:" user ":" world)
        (let [worldskey (str "worlds:" user)
              worldkey (str "world:" user ":" world)
              worldmap1 (if worldmap
                          worldmap
                          (hash-map :motd world
                                    :online "true"
                                    :private "true"))]
          (sadd worldskey world)
          (hmset worldkey worldmap1)
          (str "added world " world))))))

(defn set-userworld [user world worldmap & [oldworldmap]]
  (if (not-user? user)
    (str "error: set-userworld: user does not exist " user)
    (let [oldworldmap1 (if oldworldmap
                         oldworldmap
                         (get-userworld user world))]
      (if (nil? oldworldmap1)
        (str "error: set-userworld: world does not exist:" user ":" world)
        (let [worldkey (str "world:" user ":" world)
              oldfields (set (keys oldworldmap1))
              newfields (set (keys worldmap))
              delfields (difference oldfields newfields)]
          ;; (verbose-msg "set-userworld user=" user
          ;;              "\n world=" world
          ;;              "\n worldmap=" worldmap)
          (hmset worldkey worldmap)
          (if (not-empty delfields)
            (do
              ;; in the future this should work on a seq!
              ;; (verbose-msg " deleting fields =" delfields)
              (doseq [k delfields]
                (hdel worldkey k))))
          (update-serverchanged user)
          (str "updated world " world))))))

(defn del-userworld [user world]
  (if (nil? (get-userworld user world))
    (str "error: del-userworld: world does not exist:" user ":" world)
    (let [worldskey (str "worlds:" user)
          world? (sismember worldskey world)
          worldkey (str "world:" user ":" world)
          worldmap (hgetall worldkey)
          quietdelete (:quietdelete worldmap)]
      (if (not (empty? worldmap))
        (del worldkey))
      (srem worldskey world)
      ;; (verbose-msg "del-userworld user=" user
      ;;              "\n world=" world
      ;;              "\n worldmap=" worldmap)
      (if (not quietdelete)
        (str "deleted world " world)))))

(defn get-userinstance [])
(defn get-userinstances-set [])
(defn junjible-instance [])

(defn update-userworld [user world worldmap & [verbose-changes]]
  ;; (verbose-msg "update-userworld user=" user
  ;;              "\n world=" world
  ;;              "\n worldmap=" worldmap)
  (let [oldworldmap (get-userworld user world)
        oldname (:oldname worldmap)
        worldmap1 (dissoc worldmap :oldname)]
    (if (not-empty oldname)
      (do
        ;; we basically need to delete the oldname and add newname
        ;; AND.. if there are any cases of
        ;; instance:USER:INSTANCE . world == oldworld we nee
        ;; to update them to the new world
        (verbose-msg "renaming a world from" oldname "to" world)
        ;; NOTE: make sure the caller (e.g. panelserver.clj) will
        ;; be quiet about deleting the old one
        ;; (verbose-msg (del-userworld user oldname))
        (update-userworld user oldname
                          (merge oldworldmap (hash-map :quietdelete true)))
        (add-userworld user world worldmap1)
        (let [instances (get-userinstances-set user)]
          (doseq [instance instances]
            (let [instmap (get-userinstance user instance)
                  iworld (str (:world instmap))] ;; FORCE type to str
              (if (= iworld oldname)
                (do
                  (verbose-msg "renaming a world reference in instance" instance)
                  (update-userinstance user instance
                                       (merge instmap
                                              (hash-map :world world
                                                        :renameworld oldname))))))))
        ;; Here we know that if the oldworld was running.. the above
        ;; stopped it so we can rename the worldball here
        ;; FIXME this assumes all the worlds are archived ON the users instance
        (let [instance (first (get-userinstances-set user))
              instmap (if instance (get-userinstance user instance))
              server (:server instmap)]
          (if server
            (let [result1 (junjible-instance server
                                             (str "rename=" (URLEncoder/encode
                                                             oldname))
                                             user instance
                                             world nil)
                  success1 (= (:exit result1) 0)]
              (if success1
                (verbose-msg "renamed saved world:" result1)
                (verbose-msg "unable to rename saved world:" result1)))))
        (str "renamed world " oldname " to " world))
      (if (not oldworldmap)
        (add-userworld user world worldmap1)
        (if (= worldmap1 oldworldmap)
          (if-str verbose-changes "no change for the world " world)
          ;; (if-str (verbose?) "no change for the world " world) ;; DEBUG
          (set-userworld user world worldmap1 oldworldmap))))))

;; note: currently worlds do NOT use mixed-str-map
(defn set-userworld-field [user world & fields]
  (if (not-user? user)
    (error-msg "error: set-userworld-field: user does not exist " user)
    (if (or (nil? fields) (odd? (count fields)))
      (error-msg "error: set-userworld-field: incorrect fields setting" fields)
      (if (not (contains? (get-userworlds-set user) world))
        (error-msg "error: set-userworld-field: invalid world" world)
        (let [worldkey (str "world:" user ":" world)
              fvs (apply hash-map fields)]
          (doseq [f (keys fvs)]
            (let [v (get fvs f)]
              (verbose-msg "set-userworld-field: user" user
                           "world" world
                           "field" f
                           "value" v)
              (hset worldkey f v))))))))

(defn get-userworld-field [user world field]
  (if (not-user? user)
    (str "error: get-userworld-field: user does not exist " user)
    (if (empty? field)
      (str "error: get-userworld-field: incorrect field" field)
      (if (not (contains? (get-userworlds-set user) world))
        (str "error: get-userworld-field: invalid world" world)
        (let [worldkey (str "world:" user ":" world)
              v (hget worldkey field)]
          (verbose-msg "get-userworld-field: user" user
                       "world" world
                       "field" field
                       "value" v)
          v)))))

;; plugin-settings ---------------------------------------------------------

(defn valid-plugin? [plugin]
  (sismember "plugins" plugin))

(defn not-plugin? [plugin]
  (not (valid-plugin? plugin)))

;; takes a (potentially) str map and structures it
(defn plugin-settings-map [psmap]
  (let [screenshoturl (:screenshoturl psmap)
        settings (as-list (:settings psmap)) ;; if not nil? then it's structured
        psmap1 (dissoc psmap :screenshoturl :settings) ;; core+settings
        psmap2 (mixed-map psmap1) ;; core+settings structured
        settings1 (apply dissoc (cons psmap2 vars/*plugin-settings-keywords*))
        psmap3 (apply dissoc (cons psmap2 (difference (set (keys psmap2))
                                                      vars/*plugin-settings-keywords*)))
        ;; debug (println "DEBUG psm settings=" settings)
        ;; debug0 (println "DEBUG psm settings1=" settings1)
        ;; settings1b (for [k (keys settings1)]
        ;;              (hash-map k (get settings1 k)))
        ;; debug05 (println "DEBUG psm settings1b=" settings1b)
        settings2 (apply vector
                         (remove-nils
                          (merge-lists settings
                                       (for [k (keys settings1)]
                                         (hash-map k (get settings1 k))))))
        ;; debug1 (println "DEBUG psm settings2=" settings2)
        ;; debug2 (println "DEBUG psm screenshoturl=" screenshoturl)
        screenshoturl1 (if screenshoturl
                         (if (vector? screenshoturl)
                           screenshoturl
                           (if (> (.indexOf screenshoturl (int \|)) 0)
                             (str/split screenshoturl #"[|]")
                             (vector screenshoturl)))
                         (vector))
        ;; debug1 (println "DEBUG psm screenshoturl1=" screenshoturl1)
        psmap4 (merge psmap3
                      (hash-map :screenshoturl screenshoturl1)
                      (hash-map :settings settings2))]
    psmap4))

;; takes a (potentially) structured map and makes it a simple str map
(defn plugin-settings-str-map [psmap]
  (let [screenshoturl (:screenshoturl psmap)
        settings (as-list (:settings psmap)) ;; if not nil? then it's structured
        ;; debug (println "DEBUG pssm settings=" settings)
        psmap1 (dissoc psmap :screenshoturl :settings) ;; core+settings
        psmap2 (mixed-str-map psmap1) ;; core+settings
        settings1 (apply dissoc (cons psmap2 vars/*plugin-settings-keywords*))
        ;; debug1 (println "DEBUG pssm settings1=" settings1)
        psmap3 (apply dissoc (cons psmap2 (difference (set (keys psmap2))
                                                      vars/*plugin-settings-keywords*)))
        settings2 (merge-lists settings
                               (for [k (keys settings1)]
                                 (hash-map k (get settings1 k))))
        ;; debug2 (println "DEBUG pssm settings2=" settings2)
        settings3 (mixed-str-map (apply merge settings2))
        ;; debug3 (println "DEBUG pssm settings3=" settings3)
        screenshoturl1 (if screenshoturl
                         (if (vector? screenshoturl)
                           (str/join \| screenshoturl)
                           screenshoturl)
                         "")
        psmap4 (merge psmap3
                      (hash-map :screenshoturl screenshoturl1)
                      settings3)]
    psmap4))

;; lazy initialization
(defn initialize-plugin-settings [plugin]
  (let [pskey (str "plugin_settings:" plugin)
        psmap (hgetall pskey)
        ;; debug0 (verbose-msg "DEBUG initialize-plugin-settings psmap=" psmap)
        psmap1 (plugin-settings-map psmap)
        ;; debug1 (verbose-msg "DEBUG initialize-plugin-settings psmap1=" psmap1)
        utime (if-let [u (mixed? (:updated psmap1))] u 0)
        updated (if (> utime 0) utime (now-long))
        ;; name defaults to plugin
        pname (if-let [n (:name psmap1)] n plugin)
        ;; FIXME verify that category is in categories, default generic
        ;; debug2 (verbose-msg "DEBUG initialize-plugin-settings pname=" pname)
        category (if-let [c (:category psmap1)] c "generic")
        ;; debug3 (verbose-msg "DEBUG initialize-plugin-settings category=" category)
        available (boolean-default :available psmap1 false)
        featured (boolean-default :featured psmap1 false)
        desc (if-let [d (:desc psmap1)] d (str plugin " is a cool plugin"))
        ;; debug4 (verbose-msg "DEBUG initialize-plugin-settings desc=" desc)
        thumburl (if-let [t (:thumburl psmap1)] t
                         "/images/plugins/generic/t.jpg")
        screenshoturl (:screenshoturl psmap1)
        screenshoturl1 (if (empty? screenshoturl)
                         ["/images/plugins/generic/1.jpg",
                          "/images/plugins/generic/2.jpg",
                          "/images/plugins/generic/3.jpg"]
                         screenshoturl)
        ;; FIXME remove these example settings
        settings (:settings psmap1) ;; vector of {:s1 v1},{:s2 v2}
        ;; debug5 (verbose-msg "DEBUG initialize-plugin-settings settings=" settings)
        smap (if settings  ;; map of {:s1 v1, :s2 v2}
               (apply merge settings))
        ;; debug6 (verbose-msg "DEBUG initialize-plugin-settings smap=" smap)
        ;; nds (= (count smap) 0) ;; need default settings
        nds false ;; do NOT set default settings
        ;; debug7 (verbose-msg "DEBUG initialize-plugin-settings nds=" nds)
        setting1 (if nds true) ;; BOOLEAN
        ;; debug11 (verbose-msg "DEBUG initialize-plugin-settings setting1=" setting1)
        setting2 (if nds true) ;; BOOLEAN
        ;; debug12 (verbose-msg "DEBUG initialize-plugin-settings setting2=" setting2)
        setting3 (if nds false) ;; STRING
        ;; debug13 (verbose-msg "DEBUG initialize-plugin-settings setting3=" setting3)
        settings1 (merge (hash-map) ;; make sure there is at least an empty hash-map
                         smap
                         (if (not (nil? setting1)) (hash-map :setting1 setting1))
                         (if (not (nil? setting2)) (hash-map :setting2 setting2))
                         (if (not (nil? setting3)) (hash-map :setting3 setting3)))
        ;; debug14 (verbose-msg "DEBUG initialize-plugin-settings settings1=" settings1)
        settings2 (apply vector (for [k (sort (keys settings1))]
                                  (hash-map k (get settings1 k))))
        ;; debug15 (verbose-msg "DEBUG initialize-plugin-settings settings2=" settings2)
        psmap2 (hash-map :name pname
                         :updated updated
                         :category category
                         :available available
                         :featured featured
                         :desc desc
                         :thumburl thumburl
                         :screenshoturl screenshoturl1
                         :settings settings2)
        ;; debug20 (verbose-msg "DEBUG initialize-plugin-settings psmap2=" psmap2)
        psmap3 (plugin-settings-str-map psmap2)]
    ;; (verbose-msg "DEBUG initialize-plugin-settings psmap2=" psmap2)
    ;; (verbose-msg "DEBUG initialize-plugin-settings psmap3=" psmap3)
    (hmset pskey psmap3)
    psmap2))

;; if updating and it doesn't exist, return nil
(defn get-plugin-settings [plugin & [updating]]
  ;; REDIS (Thread/sleep 11) ;; DEBUG  
  (if (not-plugin? plugin)
    (error-msg "get-plugin-settings: plugin does not exist:" plugin)
    (let [pskey (str "plugin_settings:" plugin)
          psmap (hgetall pskey)
          updated (if-let [u (mixed? (:updated psmap))] u 0)
          psm (if (and updating (nil? psmap))
                nil
                (if (= updated 0)
                  (initialize-plugin-settings plugin)
                  (plugin-settings-map psmap)))]
      ;; (verbose-msg "DEBUG get-plugin-settings returning psm=" psm)
      psm)))

(defn add-plugin-settings [plugin & [psmap]]
  (if (not (valid-key? plugin))
    (str "error: add-plugin-settings: invalid plugin key " plugin)
    (if (not-plugin? plugin)
      (str "error: add-plugin-settings: plugin does not exist: " plugin)
      (let [pskey (str "plugin_settings:" plugin)
            neverupdated (hash-map :updated 0)
            psmap1 (if psmap
                     (merge psmap neverupdated)
                     neverupdated)
            psmap2 (plugin-settings-str-map psmap1)]
        ;; (verbose-msg "DEBUG add-plugin-settings: plugin=" plugin
        ;;              "\n psmap1=" psmap1
        ;;              "\n psmap2=" psmap2)
        (hmset pskey psmap2)
        (initialize-plugin-settings plugin)
        (str "updated settings for plugin " plugin)))))

(defn set-plugin-settings [plugin psmap]
  (if (not-plugin? plugin)
    (str "error: set-plugin-settings: plugin does not exist: " plugin)
    (let [pskey (str "plugin_settings:" plugin)
          psmap1 (plugin-settings-str-map psmap)]
      ;; (verbose-msg "set-plugin-settings pskey=" pskey
      ;;              " psmap1=" psmap1)
      (hmset pskey psmap1)
      (str "updated settings for plugin " plugin))))

(defn update-plugin-settings [plugin psmap & [verbose-changes]]
  ;; (verbose-msg "DEBUG update-plugin-settings: plugin=" plugin
  ;;              " psmap=" psmap)
  (if (not-plugin? plugin)
    (str "error: update-plugin-settings: invalid plugin: " plugin)
    (let [psmap1 (plugin-settings-map psmap)
          oldpsmap (get-plugin-settings plugin true)]
      ;; (verbose-msg "DEBUG update-plugin-setttings: psmap1=" psmap1
      ;;              " oldpsmap=" oldpsmap)
      (if (not oldpsmap)
        (add-plugin-settings plugin psmap1)
        (if (= psmap1 oldpsmap)
          (if-str verbose-changes "no settings change for plugin " plugin)
          (set-plugin-settings plugin psmap1))))))

(defn del-plugin-settings [plugin]
  ;; (verbose-msg "DEBUG del-plugin-settings: plugin=" plugin)
  (let [pskey (str "plugin_settings:" plugin)
        psmap (hgetall pskey)]
    ;; (verbose-msg "DEBUG del-plugin-setttings: psmap=" psmap)
    (if psmap
      (do
        (adel pskey)
        (str "deleted plugin settings for " plugin))
      (str "no plugin settings to delete for " plugin))))

;; plugin-user --------------------------------------------------------

;; augment and convert a str based map to a full map
(defn plugin-user-map [plugin pumap]
  (let [psmap (get-plugin-settings plugin)]
    (if (nil? psmap)
      (error-msg "cannot get user plugin settings for user with plugin:" plugin)
      (let [configured (if-let [c (:configured pumap)] c 0)
            ;; debug_ (println "DEBUG pum configured=" configured)
            activated (boolean-default :activated pumap true)
            ;; debug (println "DEBUG pum activated=" activated)
            ;; debug0 (println "DEBUG pum plugin=" plugin
            ;; "\n pumap=" pumap
            ;; "\n psmap=" psmap)
            settings (as-list (:settings pumap)) ;; previously structured settings
            ;; debug1 (println "DEBUG pum settings=" settings)
            exkeys (merge-lists (as-list vars/*plugin-settings-keywords*)
                                :configured :activated :settings)
            settings1 (mixed-map (apply dissoc (cons pumap exkeys)))
            ;; debug2 (println "DEBUG pum settings1=" settings1)
            settings2 (apply vector
                             (remove-nils
                              (merge-lists settings
                                           (for [k (keys settings1)]
                                             (hash-map k (get settings1 k))))))
            ;; debug3 (println "DEBUG pum settings2=" settings2)
            pumap1 (dissoc psmap :settings)
            pumap2 (merge pumap1
                          (hash-map :settings settings2
                                    :configured configured
                                    :activated activated))
            ;; debug4 (println "DEBUG pum pumap2=" pumap2)
            ]
        pumap2))))

;; reduce a full map to only user settings and stringify
(defn plugin-user-str-map [plugin pumap]
  (let [configured (if-let [c (:configued pumap)] c 0)
        activated (boolean-default :activated pumap true)
        ;; debug0 (println "DEBUG pusm plugin=" plugin
        ;; "\n pumap=" pumap)
        settings (as-list (:settings pumap)) ;; previously structured settings
        ;; debug1 (println "DEBUG pusm settings=" settings)
        exkeys (merge-lists (as-list vars/*plugin-settings-keywords*)
                            :configured :activated :settings)
        ;; debug15 (println "DEBUG pusm exkeys=" exkeys)
        settings1 (apply dissoc (cons pumap exkeys))
        ;; debug2 (println "DEBUG pusm settings1=" settings1)
        settings2 (merge-lists settings
                               (list (hash-map :configured configured
                                               :activated activated))
                               (for [k (keys settings1)]
                                 (hash-map k (get settings1 k))))
        ;; debug3 (println "DEBUG pusm settings2=" settings2)
        settings3 (mixed-str-map (apply merge settings2))
        ;; debug4 (println "DEBUG pusm settings3=" settings3)
        ]
    settings3))

;; initialze only user settings
(defn initialize-plugin-user [user plugin]
  (let [pukey (str "plugin_user:" user ":" plugin)
        pumap (hgetall pukey)
        ;; debug0 (verbose-msg "DEBUG initialize-plugin-user pumap=" pumap)
        ;; set configured time
        pumap1 (if pumap (mixed-map pumap))
        ctime (if-let [c (mixed? (:configured pumap1))] c 0)
        configured (if (> ctime 0) ctime (now-long))
        ;; set NOT activated on by default
        activated (boolean-default :activated pumap1 false)
        ;; choose defaults for the settings
        psmap (get-plugin-settings plugin)
        ;; debug1 (verbose-msg "DEBUG initialize-plugin-user psmap=" psmap)
        settings (:settings psmap)
        ;; debug2 (verbose-msg "DEBUG initialize-plugin-user settings=" settings)
        settings1 (apply merge settings)
        settings2 (for [k (keys settings1)] ;; set defaults based on type
                    (let [v (get settings1 k)
                          v1 (if v
                               (= (name k) "setting1") ;; BOOLEAN setting1=true, else f
                               "woogabooga")] ;; STRING default
                      (hash-map k v1)))
        ;; debug3 (verbose-msg "DEBUG initialize-plugin-user settings2=" settings2)
        settings3 (apply merge settings2)
        ;; debug35 (verbose-msg "DEBUG initialize-plugin-user settings3=" settings3)
        ;; merge in order: defaults, current settings, updated config/activate
        pumap2 (merge settings3 pumap1 (hash-map :configured configured
                                                 :activated activated))
        ;; debug4 (verbose-msg "DEBUG initialize-plugin-user pumap2=" pumap2)
        pumap3 (mixed-str-map pumap2)]
    ;; (verbose-msg "DEBUG initialize-plugin-user pumap3=" pumap3)
    (hmset pukey pumap3)
    pumap2))

(defn get-plugin-user [user plugin & [updating]]
  (if (not-user? user)
    (error-msg "get-plugin-user: invalid user:" user)
    (if (not-plugin? plugin)
      (error-msg "get-plugin-user: plugin does not exist:" plugin)
      (let [pukey (str "plugin_user:" user ":" plugin)
            pumap (hgetall pukey)
            configured (if-let [c (mixed? (:configured pumap))] c 0)
            pumap1 (if (and updating (nil? pumap))
                     nil
                     (if (= configured 0)
                       (initialize-plugin-user user plugin)
                       pumap))
            pumap2 (if pumap1 (plugin-user-map plugin pumap1))]
        ;; (verbose-msg "DEBUG get-plugin-user returning pumap2=" pumap2)
        pumap2))))

(defn add-plugin-user [user plugin & [pumap]]
  (if (not-user? user)
    (error-msg "add-plugin-user: invalid user:" user)
    (if (not-plugin? plugin)
      (str "error: add-plugin-user: plugin does not exist: " plugin)
      (let [pukey (str "plugin_user:" user ":" plugin)
            neverconfigured (hash-map :configured 0)
            pumap1 (if pumap
                     (merge pumap neverconfigured)
                     neverconfigured)
            pumap2 (plugin-user-str-map plugin pumap1)]
        ;; (verbose-msg "DEBUG add-plugin-user: user=" user
        ;;              "\n plugin=" plugin
        ;;              "\n pumap1=" pumap1
        ;;              "\n pumap2=" pumap2)
        (hmset pukey pumap2)
        (initialize-plugin-user user plugin)
        (update-serverchanged user)
        (str "updated user settings for " user " with plugin " plugin)))))

(defn set-plugin-user [user plugin pumap]
  (if (not-user? user)
    (error-msg "set-plugin-user: invalid user:" user)
    (if (not-plugin? plugin)
      (error-msg "set-plugin-user: plugin does not exist:" plugin)
      (let [pukey (str "plugin_user:" user ":" plugin)
            pumap1 (plugin-user-str-map plugin pumap)]
        ;; (verbose-msg "DEBUG set-plugin-user: user=" user
        ;;              "\n plugin=" plugin
        ;;              "\n pumap1=" pumap1)
        (hmset pukey pumap1)
        (update-serverchanged user)
        (str "updated user settings for " user " with plugin " plugin)))))

(defn update-plugin-user [user plugin pumap & [verbose-changes]]
  (if (not-user? user)
    (str "error: update-plugin-user: invalid user:" user)
    (if (not-plugin? plugin)
      (str "error: update-plugin-user: invalid plugin:" plugin)
      (let [pumap1 (plugin-user-map plugin pumap)
            oldpumap (get-plugin-user user plugin true)]
        ;; (verbose-msg "DEBUG update-plugin-user: user=" user
        ;;              "\n plugin=" plugin
        ;;              "\n pumap=" pumap
        ;;              "\n oldpumap=" oldpumap
        ;;              "\n pumap1=" pumap1)
        (if (not oldpumap)
          (add-plugin-user user plugin pumap1)
          (if (= pumap1 oldpumap)
            (if-str verbose-changes "no user settings change by " user " for plugin " plugin)
            (set-plugin-user user plugin pumap1)))))))

(defn del-plugin-user [user plugin]
  ;; (verbose-msg "DEBUG del-plugin-user: user=" user
  ;;              "\n plugin=" plugin)
  (let [pukey (str "plugin_user:" user ":" plugin)
        pumap (hgetall pukey)]
    ;; (verbose-msg "DEBUG del-plugin-user: pumap=" pumap)
    (if pumap
      (do
        (adel pukey)
        (str "deleted settings by " user " for plugin " plugin))
      (str "no user settings by " user " for plugin " plugin " to delete"))))

;; server ----------------------------------------------------

(defn initialize-server [server]
  (let [serverkey (str "server:" server)
        servermap (mixed-map (hgetall serverkey))
        utime (if-let [u (mixed? (:updated servermap))] u 0)
        updated (if (> utime 0) utime (now-long))
        host (if-let [h (:host servermap)] h "localhost")
        username (if-let [u (:username servermap)] u "junjible")
        min-port (if-let [b (:min-port servermap)] b
                         vars/*junjible-min-port*) 
        max-port (if-let [b (:max-port servermap)] b
                         vars/*junjible-max-port*) 
        max-instances (if-let [m (:max-instances servermap)] m 0)
        num-instances (if-let [n (:num-instances servermap)] n 0)
        servermap1 (hash-map :updated updated
                             :username username
                             :host host
                             :min-port min-port
                             :max-port max-port
                             :max-instances max-instances
                             :num-instances num-instances)]
    (hmset serverkey (mixed-str-map servermap1))
    servermap1))

;; CAUTION this will return ALL fields for the server
(defn get-server [server & [updating]]
  (if (empty? server)
    (error-msg "get-server: invalid server name:" server)
    (let [serverkey (str "server:" server)
          servermap (mixed-map (hgetall serverkey))
          updated (if-let [u (:updated servermap)] u 0)]
      (let [sm (if (and updating (nil? servermap))
                 nil
                 (if (= updated 0)
                   (initialize-server server)
                   servermap))]
        ;; (verbose-msg "get-server" server "servermap=" sm)
        sm))))

(defn add-server [server & [servermap]]
  (if (empty? server)
    (error-msg "refusing to add a server with an empty name")
    (let [serverkey (str "server:" server)
          unupdated (hash-map :updated 0)
          servermap1 (if servermap
                       (merge servermap unupdated)
                       unupdated)]
      (sadd "servers" server)
      (hmset serverkey (mixed-str-map servermap1))
      (initialize-server server)
      (str "added server " server))))

(defn set-server [server servermap]
  (if (empty? server)
    (error-msg "refusing to save a server with an empty name")
    (let [serverkey (str "server:" server)]
      (hmset serverkey (mixed-str-map servermap))
      ;; (verbose-msg "set-server" server "servermap=" servermap)
      (str "updated server " server))))

;; CAUTION does NOT handle cascading state change implications YET
(defn update-server [server servermap & [verbose-changes]]
  ;; (verbose-msg "DEBUG update-server: server=" server
  ;;              " servermap=" servermap)
  (let [oldservermap (get-server server true)]
    ;; (println "DEBUG update-server: oldservermap=" oldservermap)
    (if (not oldservermap)
      (add-server server servermap)
      (if (= servermap oldservermap)
        (if-str verbose-changes "no change for the server " server)
        (set-server server servermap)))))

;; instances ------------------------------------------------------------

;; check with running instance
(defn refresh-userinstance [user instance & [instmap]]
  (let [instmap1 (if instmap instmap
                     (get-userinstance user instance))
        server (:server instmap1)
        updated (if-let [u (:updated instmap1)] u 0)
        now (now-long)]
    (if (empty? server) ;; simulation mode
      instmap1
      (if (> now (+ updated vars/*instance-update-interval*))
        ;; need refresh
        (let [debug1 (verbose-msg "refresh-userinstance: user" user "instance" instance)
              exit_out_err (junjible-instance server "status" user instance nil nil)
              instmap2 (get-userinstance user instance)]
          (verbose-msg "refresh-userinstance: result=" instmap2)
          instmap2)
        (do
          (verbose-msg "refresh-userinstance: user" user
                       "instance" instance "CACHED")
          instmap1)))))

;; if refresh and the time since updated is > instance-update-interval
;; then get real status
(defn get-userinstance [user instance & [refresh]]
  (if (not-user? user)
    (error-msg "get-userinstance: user does not exist:" user)
    (let [instanceskey (str "instances:" user)
          instance? (sismember instanceskey instance)
          instkey (if instance? (str "instance:" user ":" instance))
          instmap (if instkey (mixed-map (hgetall instkey)))
          instmap1 (if refresh (refresh-userinstance user instance instmap)
                       instmap)]
      ;; (verbose-msg "get-userinstance user=" user
      ;;              "\n instance=" instance
      ;;              "\n instmap=" instmap1)
      instmap1)))

(defn get-userinstances-set [user]
  (if (not-user? user)
    (error-msg "get-userinstances-set: user does not exist:" user)
    (let [instanceskey (str "instances:" user)
          instances (apply sorted-set (smembers instanceskey))]
      instances)))

(defn add-userinstance [user instance & [ instmap ]]
  ;; (verbose-msg "add-userinstance user=" user
  ;;              "\n instance=" instance
  ;;              "\n instmap=" instmap)
  (if (not-user? user)
    (str "error: add-userinstance: user does not exist:" user)
    (if (not (valid-key? instance))
      (str "add-userinstance: invalid instance key:" instance)
      ;; if we have a non empty instmap here, we can assume that
      ;; update-userinstance is calling us -- so we know that this instance
      ;; does not exist.
      (if (and (empty? instmap) (get-userinstance user instance))
        (str "error: add-userinstance: instance already exists:" user ":" instance)
        (let [instanceskey (str "instances:" user)
              instkey (str "instance:" user ":" instance)
              instmap1 (if instmap
                         instmap
                         vars/*instance-default-settings*)]
          (sadd instanceskey instance)
          (hmset instkey (mixed-str-map instmap1))
          (str "added instance " instance))))))

(defn set-userinstance [user instance instmap & [oldinstmap]]
  (if (not-user? user)
    (str "error: set-userinstance: user does not exist " user)
    (let [oldinstmap1 (if oldinstmap
                        oldinstmap
                        (get-userinstance user instance))]
      ;; (verbose-msg "set-userinstance user=" user
      ;;              "\n instance=" instance
      ;;              "\n instmap=" instmap
      ;;              "\n oldinstmap1=" oldinstmap1)
      (if (nil? oldinstmap1)
        (str "error: set-userinstance: instance does not exist:" user ":" instance)
        (let [instancekey (str "instance:" user ":" instance)
              oldfields (set (keys oldinstmap1))
              newfields (set (keys instmap))
              delfields (difference oldfields newfields)]
          (hmset instancekey (mixed-str-map instmap))
          (if (not-empty delfields)
            (do
              ;; in the future this should work on a seq!
              ;; (verbose-msg " deleting fields =" delfields)
              (doseq [k delfields]
                (hdel instancekey k))))
          (str "updated instance " instance))))))

(defn junjible-server []
  (verbose-msg "NULL server"))

;; MUST return a (short) string with the status of the request
(defn update-userinstance [user instance instmap & [verbose-changes]]
  ;; (verbose-msg "update-userinstance user=" user
  ;;              "\n instance=" instance
  ;;              "\n instmap=" instmap)
  (let [oldinstmap (get-userinstance user instance)]
    (if (not oldinstmap) ;; no instance exists? create it!
      (add-userinstance user instance instmap)
      (if (= instmap oldinstmap)
        (if-str verbose-changes "no change for the instance " instance)
        (let [oldworld (str (:world oldinstmap)) ;; FORCE type to str
              newworld (str (:world instmap)) ;; FORCE type to str
              renameworld (:renameworld instmap)
              action (:action instmap)
              instmap1 (dissoc instmap :renameworld :action)
              updated (now-long)
              instmap2 (merge instmap1 (hash-map :updated updated))]
          ;; (verbose-msg "updating the instance for user=" user
          ;;              "\n from oldinstmap=" oldinstmap
          ;;              "\n to instmap2=" instmap2
          ;;              "\n renaming from=" renameworld)
          (if (and (empty? renameworld)
                   (not-empty oldworld)
                   (= oldworld newworld))
            (set-userinstance user instance instmap2)
            (if (not-empty action)
              (do
                (error-msg "unable to change worlds now, server is performing a"
                           action)
                (str "error: unable to change worlds now, server is performing a "
                     action))
              (let [debug (verbose-msg "world changed from"
                                       oldworld "to" newworld "STOPPING...")
                    result (junjible-server user instance "stop")
                    debug1 (verbose-msg "world change STOP result:" result)
                    success (:success result)
                    instmap3 (get-userinstance user instance)]
                ;; now the state of the instance has changed
                ;; but we STILL need to save the change in world
                (if success
                  ;; SUCCESS!
                  (do
                    (set-userinstance user instance
                                      (merge instmap3 (hash-map
                                                       :state "stopped"
                                                       :msg (str "previously was " oldworld)
                                                       :world newworld))))
                  (do
                    (error-msg "unable to stop world"
                               oldworld "for user instance" instance
                               "to change world to" newworld
                               "with result:" result)
                    ;; GO AHEAD ANYWAY!
                    ;; (str "error: unable to update instance: could not stop world " oldworld)
                    (set-userinstance user instance
                                      (merge instmap3 (hash-map
                                                       :state "stopped"
                                                       :msg (str "previously was " oldworld)
                                                       :world newworld)))))
                (if (empty? renameworld)
                  ;; we are activating a new world
                  (str "the world" oldworld
                       "has been archived, the next start will use world"
                       newworld)
                  (str "the world" oldworld
                       "has been stopped and archived, the next start will use world"
                       newworld)
                  )))))))))

(defn script [])

;; assume that the instance has a world
(defn undeploy-userinstance [user instance]
  (let [instmap (get-userinstance user instance)]
    (if (not instmap)
      (error-msg "undeploy-userinstance: instance does not exist for user"
                 user "instance:" instance)
      (let [world (str (:world instmap)) ;; FORCE type to str
            state (:state instmap)
            action (:action instmap)]
        (if (not-empty action)
          (do
            (error-msg "unable to undeploy now, server is performing "
                       action)
            (str "error: unable to undeploy now, server is performing "
                 action))
          (if (not= state "stopped")
            (let [debug0 (verbose-msg "undeploy-userinstance: stopping user"
                                      user "instance:" instance
                                      "currently in state:" state)
                  result-set (set-userinstance
                              user instance
                              (merge instmap (hash-map
                                              :undeploy true)))
                  result (junjible-server user instance "stop")
                  debug1 (verbose-msg "STOP result:" result)
                  success (:success result)]
              (if success
                (do
                  (verbose-msg "undeploy-userinstance: stopped instance")
                  (undeploy-userinstance user instance))
                (error-msg "undeploy-userinstance: unable to stop user"
                           user "instance:" instance)))
            (let [updated (now-long)
                  instmap1 (merge instmap (hash-map
                                           :updated updated
                                           :action "undeploy"))
                  server (:server instmap1)
                  servermap (get-server server)
                  host (:host servermap)
                  username (:username servermap)]
              ;; set :action
              (set-userinstance user instance instmap1)
              (let [result (script "undeploy"
                                   username host user
                                   (URLEncoder/encode world))
                    success (= (:exit result) 0)]
                (if success
                  (do
                    (set-userworld-field user world :archived true)
                    (verbose-msg "undeploy worked:" result))
                  (error-msg "undeploy failed:" result)))
              ;; clear :action
              (set-userinstance user instance instmap)
              )))))))

(defn deploy-userworld [username host user world worldsafe]
  (let [result (script "deploy"
                       username host user
                       worldsafe)
        success (= (:exit result) 0)]
    (if success
      (verbose-msg "deploy worked:" result)
      (error-msg "deploy failed:" result))
    (set-userworld-field user world :archived false)))

(defn del-userinstance [user instance]
  (if (nil? (get-userinstance user instance))
    (str "error: del-userinstance: instance does not exist:" user ":" instance)
    (let [instanceskey (str "instances:" user)
          instance? (sismember instanceskey instance)
          instkey (str "instance:" user ":" instance)
          instmap (hgetall instkey)]
      (if (not (empty? instmap))
        (del instkey))
      (srem instkey instance)
      ;; (verbose-msg "del-userinstance user=" user
      ;;              "\n instance=" instance
      ;;              "\n instmap=" instmap)
      (str "deleted instance " instance))))

;; server state ----------------------------

(defn update-serverstate [user instance state msg]
  (let [instanceskey (str "instances:" user)
        instance? (sismember instanceskey instance)
        oldinstmap (if instance? (get-userinstance user instance))
        updated (now-long)
        instmap (merge  oldinstmap (hash-map :state state
                                             :msg msg
                                             :updated updated))]
    ;; (verbose-msg "update-serverstate instance=" instance " = " instmap)
    (if instance?
      (set-userinstance user instance instmap))))

(defn update-serverchanges [user changes]
  (let [instanceskey (str "instances:" user)
        instances (smembers instanceskey)
        instance (first instances) ;; just update the first one!! FIXME
        oldinstmap (if instance? (get-userinstance user instance))
        instmap (merge oldinstmap (hash-map :changes changes))]
    ;; (verbose-msg "update-serverchanges user=" user " instance=" instance)
    (if (not= instmap oldinstmap)
      (set-userinstance user instance instmap))))

(defn update-serverchanged [user]
  (update-serverchanges user true))

(defn junjible-instance-whitelist [port whitelist]
  (error-msg "junjible-instance-whitelist: not reimplemented yet"))
;; NOTE: remove CAVA envars
;;   (let [whitedir (str (System/getenv "CAVAINSTANCES") "/" port)
;;         whitefile (str whitedir "/white.txt")]
;;     (mkdirp whitedir)
;;     (utils/with-out-file whitefile
;;       (doseq [w whitelist]
;;         (println w)))
;;     whitefile))

(defn server-response [exit out err]
  (hash-map :exit exit :out out :err err))

(defn server-error [& args]
  (server-response 1 "" (apply str args)))

(defn max-str [s]
  (let [maxlen 100
        len (count s)
        s1 (if (> len maxlen) (.substring s 0 maxlen) s)]
    s1))

(defn script [& cmdargs]
  (let [command (first cmdargs)
        program (str "./bin/" command)
        args (rest cmdargs)
        cmd (merge-lists program args :in "")]
    (verbose-msg "cmd:" cmd)
    (try
      (let [result (apply sh cmd)]
        (verbose-msg "result:" result)
        result)
      (catch Exception e
        (error-msg "result: EXCEPTION: " e "\n")
        (server-response 255 "error" "unable to run script")))))

;;=================

(defn get-permissions []
  (boolean-map (hgetall "permissions")))

(defn set-permissions [perms]
  (let [oldperms (hgetall "permissions")
        msg (str "set-permissions to " perms)]
    (if oldperms
      (del "permissions"))
    (hmset "permissions" (boolean-str-map perms))
    msg))

(defn add-permission [perm & [specialp]]
  (let [special (is-true? specialp)
        oldperms (get-permissions)
        kperm (keyword perm)]
    (if (contains? oldperms kperm)
      (str "add-permission: permission already exists:" kperm)
      (let [perms (merge oldperms (hash-map kperm special))]
        (set-permissions perms)
        (str "added permission " perm ": " special)))))

(defn del-permission [perm]
  (let [oldperms (get-permissions)
        kperm (keyword perm)]
    (if (contains? oldperms kperm)
      (let [perms (dissoc oldperms kperm)]
        (set-permissions perms)
        (str "removed permission " perm))
      (str "del-permission: permission does not exist:" perm))))

(defn update-permissions [perms & [verbose-changes]]
  (let [verbose (or verbose-changes (verbose?))]
    ;; (println "debug update-permissions perms class:" (.getClass perms)
    ;;          "vector?" (vector? perms)
    ;;          "seq?" (seq? perms)
    ;;          "map?" (map? perms))
    (if (seq? perms)
      (if (odd? (count perms))
        (str "ERROR update-permissions: even number of arguments required")
        (update-permissions (boolean-map (keywordize (apply hash-map perms))) verbose-changes))
      (let [oldperms (get-permissions)]
        ;; (println "debug update-permissions perms:" perms)
        ;; (println "debug update-permissions oldperms:" oldperms)
        (if (= perms oldperms)
          (if verbose
            (str "no change for permissions"))
          (do
            (set-permissions perms)
            (if verbose
              (str "updated-permissions:" perms))))))))

(defn get-categories []
  (apply sorted-set (smembers "categories")))

(defn update-categories [categories & [verbose-changes]]
  (let [verbose (or verbose-changes (verbose?))]
    (if (not (set? categories))
      (update-categories (set categories) verbose)
      (let [oldcategories (get-categories)]
        ;; (verbose-msg "debug update-categories categories:" categories)
        ;; (verbose-msg "debug update-categories oldcategories:" oldcategories)
        (if (= categories oldcategories)
          (if verbose
            (str "no change for categories"))
          (do
            (if (not (empty? oldcategories))
              (del "categories"))
            (sadd "categories" categories)
            (if verbose
              (str "updated-categories"))))))))
;; make the message short (as it may get routed back to the UI
;;            (str "updated-categories:" categories))))))))

;; NOTE: the sort order is important when we are paging
;; sets of plugins
(defn get-plugins []
  (apply sorted-set (smembers "plugins")))

(defn update-plugins [plugins & [verbose-changes]]
  (let [verbose (or verbose-changes (verbose?))]
    (if (not (set? plugins))
      (update-plugins (set plugins) verbose)
      (let [oldplugins (get-plugins)]
        ;; (verbose-msg "debug update-plugins plugins:" plugins)
        ;; (verbose-msg "debug update-plugins oldplugins:" oldplugins)
        (if (= plugins oldplugins)
          (if-str verbose "no change for plugins")
          (do
            (if (not (empty? oldplugins))
              (del "plugins"))
            (sadd "plugins" plugins)
            (if verbose
              (str "updated plugins"))))))))

(defn update-perms [ugkey user ug perms]
  (let [pkey (str ugkey ":" user ":" ug)
        oldperms (smembers pkey)
        newperms (set perms)]
    (verbose-msg "update-perms" pkey "=" newperms)
    (verbose-msg "oldperms = " oldperms)
    (if (= newperms oldperms)
      "no permissions change"
      (do
        (if (not (empty? oldperms))
          (del pkey))
        (sadd pkey newperms)
        (update-serverchanged user)
        "updated permissions"))))

;; gets the permissions for a plugin
(defn get-pluginperms [plugin]
  (let [k (str "plugin_perms:" plugin)]
    (apply sorted-set (smembers k))))

(defn update-pluginperms [plugin perms & [verbose-changes]]
  (let [verbose (or verbose-changes (verbose?))]
    (if (not-plugin? plugin)
      (error-msg "update-pluginperms: plugin does not exist:" plugin)
      (if (not (set? perms))
        (update-pluginperms plugin (set perms) verbose)
        (let [ppkey (str "plugin_perms:" plugin)
              oldperms (get-pluginperms ppkey)]
          (if (= perms oldperms)
            (if-str verbose "no change of permissions for plugin " plugin)
            (do
              (if (not (empty? oldperms))
                (del ppkey))
              (sadd ppkey perms)
              (str "updated permissions for plugin " plugin))))))))

(defn get-allpluginperms []
  (let [plugins (get-plugins)
        pperms (set (apply union (for [p plugins] (get-pluginperms p))))]
    (verbose-msg "get-allpluginperms = " pperms)
    pperms))

;; helper function that gets the union of all possible permissions
(defn get-allperms []
  (let [pluginperms (get-allpluginperms)
        kperms (keys (get-permissions))
        perms (set (for [k kperms] (name k)))]
    (union perms pluginperms)))

;; helper function that returns the set of true plugin perms
(defn json-pluginperms [plugin]
  (let [plugins (vals plugin)]
    (apply concat (remove-nils
                   (for [plugin plugins]
                     (for [k (keys plugin)]
                       (if (is-true? (get plugin k)) (name k))))))))

;; The purpose in filtering on allperms is to make sure we don't end up
;; with some random permissions back in the database.
;; NOTE: (permissions union plugin_perms:*) must be unique
;; /server/save-permissions for tmarble = {:group , :member fred, :basic {11 [tp true], 12 [save true], 0 [gamemode true], 1 [kick true], 2 [give true], 3 [breakspawn true], 4 [xp true], 5 [say true], 6 [wand true], 7 [pardon true], 8 [stop true], 9 [time true], 10 [weather false]}, :special {0 [ban true]}, :plugin {0 {:plugperms {0 [splash false], 1 [take_a_bath true], 2 [splish true]}, :name superplug}, 1 {:plugperms {0 [happiness true], 1 [liberty true], 2 [life true]}, :name wonderplug}, 2 {:plugperms {0 [hyperventilate true], 1 [itch true], 2 [bleed true]}, :name nastyplug}}}
;; group=  member= fred special= {:ban true} basic= {:gamemode true, :kick true, :give true, :breakspawn true, :xp true, :say true, :wand true, :pardon true, :stop true, :time true, :weather false, :tp true, :save true} plugin= {:superplug {:splash false, :splish true, :take_a_bath true}, :nastyplug {:hyperventilate true, :itch true, :bleed true}, :wonderplug {:liberty true, :life true, :happiness true}}
(defn json-userpermissions [user group member special basic plugin]
  (let [specials (for [k (keys special)]
                   (if (is-true? (get special k)) (name k)))
        basics (for [k (keys basic)]
                 (if (is-true? (get basic k)) (name k)))
        pluginperms (json-pluginperms plugin)
        kperms (remove-nils (concat specials basics pluginperms))
        allperms (get-allperms)
        perms (set (remove-nils (for [k kperms] (if (contains? allperms k) k))))]
    ;; (println "specials =" specials)
    ;; (println "basics =" basics)
    ;; (println "pluginperms =" pluginperms)
    ;; (println "kperms =" kperms)
    ;; (println "allperms =" allperms) ;; DEBUG
    ;; (println "perms =" perms) ;; DEBUG
    (cond (and (empty? group) (not (empty? member)))
          (do
            ;; (verbose-msg "json-userpermissions for member: " member " = " perms)
            ;; (str "json-userpermissions for member: " member " = " (str-realize perms))
            (update-perms "user_perms" user member perms)
            )
          (and (not (empty? group)) (empty? member))
          (do
            ;; (verbose-msg "json-userpermissions for group: " group " = " perms)
            ;; (str "json-userpermissions for group: " group " = " (str-realize perms))
            (update-perms "group_perms" user group perms)
            )
          true
          "ERROR: json-userpermissions: may only update member OR group")))

(defn get-perms [ugkey user ug]
  (let [pkey (str ugkey ":" user ":" ug)
        perms (smembers pkey)]
    ;; (verbose-msg "get-perms" pkey "=" perms)
    perms))

(defn get-userpermissions [user group member]
  (cond (and (empty? group) (not (empty? member)))
        (get-perms "user_perms" user member)
        (and (not (empty? group)) (empty? member))
        (get-perms "group_perms" user group)
        true
        "ERROR: get-userpermissions: may only get member OR group"))

;; returns nil if not available and activated
(defn activated-plugin [user plugin]
  (let [pumap (get-plugin-user user plugin)
        ;; debug (verbose-msg "activated-plugin " user ":" plugin "=" pumap)
        available (:available pumap)
        activated (:activated pumap)]
    (if (and available activated)
      plugin)))

(defn activated-plugins [user]
  (if (not-user? user)
    (str "error: activated-plugins: invalid user:" user)
    (let [allplugins (get-plugins)
          plugins (remove-nils (for [plugin allplugins]
                                 (activated-plugin user plugin)))]
      ;; (str "active plugins for " user " = " (str-realize plugins)))))
      (str/join \, plugins))))

(defn get-userplugins-count []
  (let [plugins (get-plugins)
        n (reduce #'+
                  (for [plugin plugins
                        :let [a (:available (get-plugin-settings plugin))]]
                    (if a 1 0)))]
    n))

(defn get-userplugins-page [user plugins & [page]]
  (let [n (get-userplugins-count)
        j (if (nil? page) 0 (* page 10))
        k (if (nil? page) n (min n (* (inc page) 10)))
        p (remove-nils
           (for [plugin plugins]
             (let [pumap (get-plugin-user user plugin)
                   available (:available pumap)]
               (if available (merge pumap (hash-map :plugin plugin))))))] ;; add id
    (for [i (range j k)] (nth p i))))

;; ---------------------------------------------

(defn min-max-ports [min-port max-port]
  (let [min-p (if min-port min-port vars/*junjible-min-port*)
        max-p (if max-port max-port vars/*junjible-max-port*)
        min-po (if (odd? min-p) (inc min-p) min-p)
        max-po (if (odd? max-p) (dec max-p) (- max-p 2))]
    (list min-po max-po)))

(defn find-server-port [server port]
  (let [sikey (str "server_instance:" server ":" port)
        sival (rget sikey)]
    (if (empty? sival) ;; not allocated and not reserved
      (list server port))))

(defn find-server-instance [server min-port max-port pref-port & [tried]]
  (if (and pref-port
           (empty? tried)
           (>= pref-port min-port)
           (<= pref-port max-port))
    (if-let [server-port (find-server-port server pref-port)]
      server-port
      (find-server-instance server min-port max-port nil (set pref-port)))
    (let [n (inc (/ (- max-port min-port) 2))
          port (+ min-port (* (rand-int n) 2))]
      (if (and (not-empty tried) (contains? tried port))
        (find-server-instance server min-port max-port nil tried)
        (if-let [server-port (find-server-port server port)]
          server-port
          (find-server-instance server min-port max-port nil
                                (conj (set tried) port)))))))


(defn unassign-instance [])

;; tries to unassign the oldest instance
;; return true on success
(defn unassign-oldest-instance [server]
  (let [stops (let [re (re-pattern (str "^server_instance:" server ":.*"))]
                (for [k (sort (rkeys))]
                  (if (re-matches re k)
                    (let [;; si-s-p (str/split k #"[:]")
                          ;; port (nth si-s-p 2 nil)
                          sival (rget k)
                          userinst (str/split sival #"[:]")
                          user (first userinst)
                          instance (nth userinst 1 nil)
                          instmap (get-userinstance user instance)
                          updated (if-let [u (:updated instmap)] u 0)
                          stopped (= (:state instmap) "stopped")]
                      (if stopped
                        (hash-map updated user))))))
        allstopped (apply merge (remove-nils stops))
        oldest (first (sort (keys allstopped)))
        oldest-user (get allstopped oldest)]
    (if oldest-user
      (do
        (verbose-msg "unassign-oldest-instance for user:" oldest-user)
        (unassign-instance oldest-user)
        true))))

(defn find-available-instance [pref-server pref-port & [tried-unassign]]
  (let [servers-unsorted (set (get-servers))
        servers (if (and (not-empty pref-server)
                         (contains? servers-unsorted pref-server))
                  (cons pref-server (disj servers-unsorted pref-server))
                  servers-unsorted)]
    (verbose-msg "find-available-instance servers=" servers)
    (first
     (remove-nils
      (for [server servers]
        (let [servermap (get-server server)
              min-max (min-max-ports (:min-port servermap)
                                     (:max-port servermap))
              min-port (first min-max)
              max-port (first (rest min-max))
              max-instances (:max-instances servermap)
              num-instances (:num-instances servermap)
              free-instances (- max-instances num-instances)]
          (if (> free-instances 0)
            (do
              (verbose-msg "server" server "has" free-instances "available")
              (find-server-instance server min-port max-port pref-port))
            (if (and (not tried-unassign) (unassign-oldest-instance server))
              (find-available-instance pref-server pref-port true)
              (do
                (verbose-msg "server" server "has no available instances")
                nil)))))))))

;; THE return value is different based on success
;; string? is true on failure -- with an error message, else
;; it is a list with the server and the instance number on the server
;; '(server n)
(defn assign-instance [user]
  (if (not-user? user)
    (str "error: assign-interface: invalid user:" user)
    (let [instanceskey (str "instances:" user)
          instances (smembers instanceskey)
          instance (first instances) ;; just update the first one!! FIXME
          instmap (if instance (get-userinstance user instance))
          pref-server (:pref-server instmap)
          pref-port (:pref-port instmap)]
      (if (empty? instmap)
        (str "error: assign-interface: no instance defined")
        (if (not (empty? (:server instmap)))
          (str "error: assign-interface: instance already assigned to server: "
               (:server instmap))
          (let [server-port (find-available-instance pref-server pref-port)
                server (first server-port)
                port (first (rest server-port))]
            (if (nil? port)
              (if (empty? (get-servers))
                ;; special case: simulate a server if NONE defined
                "error: no servers defined"
                "error: all servers busy")
              (let [sikey (str "server_instance:" server ":" port)
                    debug (verbose-msg "sikey=" sikey)
                    sival (str user ":" instance)
                    servermap (get-server server)
                    num-instances (inc (:num-instances servermap))
                    servermap1 (merge servermap (hash-map
                                                 :num-instances num-instances))
                    serverhost (:host servermap1)
                    instmap1 (merge instmap (hash-map
                                             :server server
                                             :host serverhost
                                             :port port
                                             :pref-server server
                                             :pref-port port
                                             ))]
                (rset sikey sival)
                (update-server server servermap1)
                (set-userinstance user instance instmap1)
                (verbose-msg "assigned" sival "to" sikey)
                (list server port)))))))))

(defn unassign-instance [user]
  (if (not-user? user)
    (str "error: unassign-interface: invalid user:" user)
    (let [instanceskey (str "instances:" user)
          instances (smembers instanceskey)
          instance (first instances) ;; just update the first one!! FIXME
          instmap (if instance (get-userinstance user instance))
          server (if instmap (:server instmap))
          port (if instmap (:port instmap))]
      (if (empty? instmap)
        (do
          (error-msg "error: unassign-interface: no instance defined")
          (str "error: unassign-interface: no instance defined"))
        (if (empty? server)
          (do
            (error-msg "error: unassign-interface: instance not assigned")
            (str "error: unassign-interface: instance not assigned"))
          (do
            (verbose-msg "unassign-instance for "
                         "server" server
                         "user" user
                         "instance" instance)
            (undeploy-userinstance user instance)
            (let [sikey (str "server_instance:" server ":" port)
                  ;; sival (str user ":" instance)
                  servermap (get-server server)
                  num-instances (max 0 (dec (:num-instances servermap)))
                  servermap1 (merge servermap (hash-map
                                               :num-instances num-instances))
                  instmap1 (merge instmap (hash-map
                                           :server ""
                                           :host ""
                                           :port ""))]
              (verbose-msg "unassigning sikey:" sikey)
              (del sikey)
              (update-server server servermap1)
              (set-userinstance user instance instmap1)
              (verbose-msg "unassigned" sikey "from" user "instance" instance)
              true)))))))

;; removes all assignments for a server
(defn unassign-server [server]
  (verbose-msg "unassigning all work from server" server)
  (let [re (re-pattern (str "^server_instance:" server ":.*"))]
    (doseq [k (sort (rkeys))]
      (if (re-matches re k)
        (let [sival (rget k)
              userinst (str/split sival #"[:]")
              user (first userinst)
              instance (nth userinst 1 nil)]
          (verbose-msg "found matching key" k "= user:" user "instance:" instance)
          (unassign-instance user)
          )))
    true))

;;  REMOVE servers SERVER -> any key maching *:SERVER$ or *:SERVER:
;;  CAUTION does NOT handle cascading state change implications YET
(defn del-server [server]
  (verbose-msg "deleting server" server)
  (unassign-server server)
  (let [serverkey (str "server:" server)]
    ;; servermap (mixed-map (hgetall serverkey))]
    (srem "servers" server)
    (adel serverkey)
    (str "deleted server " server)))

;; BE AWARE.. this function is to be called from a future
;; WITHOUT any bindings.. notably NOT the redis-db binding
;; NOR any *out* bindings
;; That's why we are using globals
;; (defn consider-undeploy [server user instance world updated]
;; keep err binding so we can use verbose-msg
(defn consider-undeploy [user instance lastupdated err]
  ;; (redis/with-server (redis-options)
  ;;   (set-global "undeploy"
  ;;               (str "undeploy"
  ;;                    " server " server
  ;;                    " user " user
  ;;                    " instance " instance
  ;;                    " world " world
  ;;                    " updated " updated)))
  (Thread/sleep vars/*undeploy-after-stop*)
  (binding [*err* err]
    (redis/with-server (redis-options)
      ;; (let [prev (get-global "undeploy")
      ;;       val (str prev " done " (now-long))]
      ;;   (set-global "undeploy" val))
      (let [instmap (get-userinstance user instance)
            updated (:updated instmap)
            state (:state instmap)]
        (if (or (not= state "stopped")
                (> updated (+ lastupdated vars/*instance-update-epsilon*)))
          ;; there has been activity since last updated
          (verbose-msg "consider-undeploy user" user
                       "instance" instance
                       "ABORTED due to activity")
          ;; there has been NO substantial activity since last updated
          (do
            (verbose-msg "consider-undeploy user" user
                         "instance" instance
                         "UNDEPLOY")
            (undeploy-userinstance user instance)))))))

(defn junjible-instance [server action user instance world worldmap]
  (if (empty? server)
    (server-response 1 "error" "junjible-instance: cannot run with empty server name")
    (let [instmap (get-userinstance user instance)
          undeploy (:undeploy instmap)
          set-result (set-userinstance user instance
                                       (merge instmap (hash-map :action action)))
          servermap (get-server server)
          ;; debug (verbose-msg "junjible-instance servermap=" servermap)
          username (:username servermap)
          host (:host instmap)
          usermap (get-user user)
          minecraft (if-let [m (:minecraft usermap)] m user)
          white minecraft ;; add the users mineraft name to the whitelist
          port (str (:port instmap))
          maxheap (if-let [mh (:maxheap instmap)] mh "1024m")
          motd (if-let [m (:motd worldmap)] m "")
          motdsafe (if (not-empty motd) (str "'" (URLEncoder/encode motd) "'"))
          worldsafe (if (not-empty world) (URLEncoder/encode world))
          archived (is-true? (:archived worldmap)) ;; worlds are not mixed
          online (is-true? (:online worldmap)) ;; worlds are not mixed
          private (is-true? (:private worldmap)) ;; worlds are not mixed
          plugins (activated-plugins user)
          maxplayers (str 10) ;; FIXME
          cmdverbose false ;;(and verbose? false)
          javastats true
          updated (now-long)
          configure? (or (= action "start") (= action "restart"))
          cmd (remove-nils (list
                            "ssh"
                            "-n"
                            (str username "@" host)
                            vars/*junjible-script*
                            (if cmdverbose "-d")
                            (if cmdverbose "-v")
                            (if javastats "-j")
                            "-u" user
                            (if worldsafe "-W")
                            (if worldsafe worldsafe)
                            (if port "--port")
                            (if port port)
                            (if (and configure? maxheap) "--max-heap")
                            (if (and configure? maxheap) maxheap)
                            (if (and configure? motdsafe) "--motd")
                            (if (and configure? motdsafe) motdsafe)
                            (if (and configure? white) "--whitelist")
                            (if (and configure? white) white)
                            (if (and configure? maxplayers) "-P")
                            (if (and configure? maxplayers) maxplayers)
                            (if configure? "--online-mode")
                            (if configure? (is-true-str? online))
                            (if (and configure? (not-empty plugins)) "--plugins")
                            (if (and configure? (not-empty plugins)) plugins)
                            action
                            :in ""))]
      (if (and configure? archived)
        (deploy-userworld username host user world worldsafe))
      (verbose-msg "cmd:" cmd)
      (try
        (let [result (apply sh cmd)
              exit (:exit result)
              outmsg (str/trim (max-str (:out result)))
              errmsg (str/trim (max-str (:err result)))
              success (or (and (= exit 1)
                               (= action "status")
                               (= outmsg "stopped"))
                          (= exit 0))
              state (if success
                      (if (.startsWith action "rename=")
                        "stopped" outmsg)
                      "error")
              msg (if success "" errmsg)
              instmap1 (merge instmap (hash-map :state state
                                                :msg msg
                                                :changes false
                                                :updated updated))
              instmap2 (dissoc instmap1 :action :undeploy)] ;; clear action
          (verbose-msg "result:" result)
          (if (and success (= action "stop") (not (true? undeploy)))
            (do
              (verbose-msg "starting future thread to undeploy"
                           "server" server
                           "user" user
                           "instance" instance
                           "world" world
                           "updated" updated)
              ;; we never deref the "value" of this function
              ;; (future (consider-undeploy server user instance world updated *err*))))
              (future (consider-undeploy user instance updated *err*))))
          (set-userinstance user instance instmap2)
          (server-response exit outmsg errmsg))
        (catch Exception e
          ;; (verbose-msg "result: EXCEPTION: " (.getMessage e))
          (error-msg "result: EXCEPTION: " e "\n")
          (set-userinstance
           user instance
           (dissoc (merge instmap
                          (hash-map :state "error"
                                    :msg "unable to run server"
                                    :updated updated))
                   :action)) ;; clear action
          (server-response 255 "error" "unable to run server"))))))

(defn junjible-instance-simulate [action user instance]
  (let [instmap (get-userinstance user instance)
        set-result (set-userinstance user instance
                                     (merge instmap (hash-map :action action)))
        host "localhost"
        port 25565
        oldstate (:state instmap)
        ;; every other time we will fail
        success (= oldstate "error")
        state (if success
                (if (= action "stop") "stopped" "running")
                "error")
        msg (if success "simulate normal" "simulate failing")
        updated (now-long)
        instmap1 (merge instmap (hash-map :server ""
                                          :host host
                                          :port port
                                          :state state
                                          :msg msg
                                          :changes false
                                          :updated updated))]
    (verbose-msg "simulating server" action "for" user "result:" msg)
    (Thread/sleep 10000) ;; 10 seconds
    (set-userinstance user instance
                      (dissoc instmap1 :action)) ;; clear action
    (if success
      (server-response 0 state "")
      (server-response 1 "" msg))))

;; returns 1 if allocated, 0 if not allocated
(defn junjible-instance-verify-instances [server servermap port-status]
  (verbose-msg "junjible-instance-verify-instances:" server)
  (let [sikeys (rkeys (str "server_instance:" server ":*"))]
    (for [sikey sikeys]
      (let [sival (rget sikey)]
        (if (and (not-empty sival) (not= sival "reserved"))
          (let [port (nth (str/split sikey #"[:]") 2 nil)
                userinst (str/split sival #"[:]")
                user (first userinst)
                instance (nth userinst 1 nil)
                instmap (if (and user instance)
                          (get-userinstance user instance))
                oldstate (:state instmap)
                state (if (= (get port-status (str port)) "running")
                        "running" "stopped")
                updated (now-long)]
            (if (not-empty instmap)
              (do
                (if (= oldstate state)
                  (verbose-msg "state unchanged for server" server
                               "port" port
                               "for user" user
                               "instance" instance
                               "now" state)
                  (do
                    (set-userinstance user instance
                                      (merge instmap (hash-map
                                                      :state state
                                                      :updated updated)))
                    (verbose-msg "updating state for server" server
                                 "port" port
                                 "for user" user
                                 "instance" instance
                                 "currently" state)))
                1) ;; ALLOCATED
              0)) ;; UNALLOCATED
          0))))) ;; UNALLOCATED

;; MAKE sure any of the instances for this server do NOT
;; have an action pending
(defn clear-instance-actions [server]
  (verbose-msg "clear-instance-actions for server:" server)
  (let [sikeys (rkeys (str "server_instance:" server ":*"))]
    (doseq [sikey sikeys]
      (let [sival (rget sikey)]
        (if (and (not-empty sival) (not= sival "reserved"))
          (let [userinst (if sival (str/split sival #"[:]"))
                user (first userinst)
                instance (nth userinst 1 nil)
                instmap (if (and user instance) (get-userinstance user instance))]
            (if (not-empty instmap)
              (do
                (verbose-msg (str "clearing action for \"" sikey
                                  "\" user \"" user
                                  "\" instance \"" instance "\""))
                (update-userinstance user instance (dissoc instmap :action))))))))))

(defn junjible-instance-verify [server & [ servermap ]]
  (verbose-msg "verifing state of server:" server)
  (let [servermap1 (if servermap servermap (get-server server))
        username (:username servermap1)
        host (:host servermap1)
        cmd (remove-nils (list
                          "ssh"
                          "-n"
                          (str username "@" host)
                          vars/*junjible-script*
                          ;; "-v" ;; DEBUG
                          ;; "-d" ;; DEBUG
                          "verify"
                          :in ""))]
    (verbose-msg "cmd:" cmd)
    (try
      (let [result (apply sh cmd)
            exit (:exit result)
            out (:out result)]
        (if (not= exit 0)
          (do ;; SERVER STATUS FAILED
            (verbose-msg "server " server "@" host
                         " is not responding or not configured properly")
            ;; set verified time to zero
            (update-server server (merge servermap1
                                         (hash-map :verified 0))))
          (let [some (> (count out) 0)
                ports (if some (str/split out #"\n"))
                status (if ports (for [p ports] (apply hash-map (str/split p #"\t"))))
                port-status (if status (apply merge status) (hash-map))
                max-instances (:max-instances servermap1)
                num-instances (:num-instances servermap1)
                verified (junjible-instance-verify-instances
                          server servermap1 port-status)
                num-verified (reduce #'+ verified)
                servermap2 (merge servermap1
                                  (hash-map :verified (now-long)
                                            :num-instances num-verified))]
            (if (not= num-verified num-instances)
              (verbose-msg "the actual number of allocated instances is"
                           num-verified))
            (update-server server servermap2)
            (clear-instance-actions server))))
      (catch Exception e
        (print "unable to get server status for server" server
               "@" host
               "EXCEPTION: " e "\n")))))

(defn valid-action? [action]
  (and (string? action)
       (or (.startsWith action "rename=")
           (get vars/*server-actions* action))))

(defn junjible-server [user instance action]
  (verbose-msg "=== server" user instance action "===")
  (let [exit_out_err
        (if (not (valid-key? instance))
          (server-error "invalid instance key:" instance)
          (if (not-user? user)
            (server-error "user does NOT exist:" user)
            (if (not (valid-action? action))
              (server-error "invalid action:" action)
              (let [instanceskey (str "instances:" user)
                    instance? (sismember instanceskey instance)]
                (if (not instance?)
                  (server-error "instance does not exist:" user ":" instance)
                  (let [instmap (get-userinstance user instance)
                        prevaction (:action instmap)
                        server (:server instmap)
                        world (str (:world instmap)) ;; FORCE type to str
                        worldmap (if world (get-userworld user world))
                        server-port (if (empty? server) (assign-instance user))
                        debug (verbose-msg (str "server-port \"" server-port "\""))]
                    (if (not-empty prevaction)
                      (server-error (str "unable to " action
                                         " now, server is performing a " prevaction))
                      (if (servers-locked?)
                        (server-error (str "unable to " action
                                           " now, Minecraft server unavailable: "
                                           (servers-locked?)))
                        (if (empty? worldmap)
                          (do
                            (verbose-msg "instance does not specify world:" user ":" instance)
                            (server-error "please click a world to activate!"))
                          (if (and (empty? server) (string? server-port))
                            ;; we could not allocate a server
                            (if (= server-port "error: no servers defined")
                              (junjible-instance-simulate action user instance)
                              ;; we have defined servers and none are available
                              (server-error server-port))
                            ;; here we know we have a server allocated
                            (let [server1 (if (empty? server)
                                            (first server-port)
                                            server)]
                              (verbose-msg (str "calling junjible-instance server \""
                                                server1
                                                "\" action \"" action
                                                "\" user \"" user
                                                "\" instance \"" instance
                                                "\" world \"" world
                                                "\"..."))
                              (junjible-instance server1 action user instance world worldmap))))))))))))
        exit (:exit exit_out_err)
        outmsg (:out exit_out_err)
        errmsg (:err exit_out_err)
        success (= exit 0)
        state (if success outmsg "error")
        msg (if success "" errmsg)
        result (hash-map :success success :state state :msg msg)]
    (update-serverstate user instance state msg)
    result))

;; cavapanel specific functions --------------------------------------------

(defn get-allowed-emails []
  (let [emails (smembers "allowed:emails")]
    (apply sorted-set   
           (for [email emails] (str/lower-case email)))))

(defn emailused? [email]
  (let [users (smembers "users")
        email1 (str/lower-case email)]
    (if (empty? users)
      false
      (do
        ;; (verbose-msg "emailused?" users " email:" email)
        (reduce or-all
                (for [u users] (= email1 (str/lower-case (hget (str "user:" u) :email)))))))))

(defn login? [{:keys [user pass remember]}]
  (let [usermap (get-user user)]
    ;; (println "login? user:" user " pass:" pass " remember:" remember)
    ;; (println "login? usermap=" usermap)
    (if (= (:pass usermap) pass)
      (let [expiry (time-format/unparse
                    (:rfc822 time-format/formatters)
                    (time/plus (time/now) (time/days 1)))
            lastuser (if remember user "")]
        ;; (println "setting cookie :cavapanel-lastuser =" lastuser)
        (session/put! :cavapanel-user (str/trim user))
        (cookies/put! :cavapanel-lastuser 
                      {:value lastuser
                       :path "/"
                       :expires expiry})
        true)
      false)))

(defn user? []
  (let [user (session/get :cavapanel-user)]
    (if (empty? user)
      nil
      (str/trim user))))

(defn lastuser []
  (let [lastuser (cookies/get :cavapanel-lastuser)]
    ;; (println "lastuser:" lastuser)
    ;; (println "seq:" (seq lastuser))
    lastuser))
;; (if (seq lastuser)
;;   lastuser
;;   "")))

(defn logout []
  (if (user?)
    (session/put! :cavapanel-user "")))

;; this is a server startup hook =====================================

(defn initialize-cavapanel [verify]
  ;; REDIS
  (redis/with-server (redis-options)
    (verbose-msg "initialize-cavapanel...")
    (verbose-msg "sample salt:" (pw/random256-base64-str))
    (swap! vars/*redis-ops*
           use-new
           {:set {:list smembers, :del del, :add sadd},
            :hash {:list hgetall, :del del, :add hset},
            nil {:list rget, :del del, :add rset}})
    ;; verify servers are online
    (if verify
      (let [start (now-long)
            servers (get-servers)]
        (swap! vars/*start* use-new start)
        (verbose-msg "server started at" 
                     (time-format/unparse
                      (:rfc822 time-format/formatters)
                      (time-convert/from-long start)))
        (if (empty? servers)
          (verbose-msg "WARNING: no servers defined.. will simulate server behavior!")
          (do
            (verbose-msg "Verifying the status of the following servers:" servers)
            (doseq [server servers]
              (let [servermap (get-server server)
                    verified (as-long (:verified servermap))]
                (if (and verified (> verified start))
                  (verbose-msg "server has been verified: " server)
                  (do ;; verify
                    (junjible-instance-verify server servermap)
                    ))))))))
    ;; (verbose-msg "...ready")
    ))

