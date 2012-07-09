(ns cavapanel.database-actions
  (:require [clojure.string :as str]
            [cavapanel.vars :as vars]
            [cavapanel.database :as db])
  (:use ;; [clojure.set]
           [redis.core :only [with-server]]
           [cavapanel.utils]))

; dispatch on key type
(defmulti junjible-db-action
  "operate on the cavapanel database"
  (fn [args] (keyword (str/lower-case (if-let [f (first args)] f "default")))))
  ;; (fn [args] (let [k (keyword (str/lower-case (if-let [f (first args)] f "default")))]
  ;;            (println "dispatching on:" args "k:" k)
  ;;            k)))

(defmethod junjible-db-action :listall [args]
  ;; (println "junjible-db: listall:" args)
  (doseq [k (sort (db/rkeys))]
    (let [kbase (first (str/split k #":"))
          ktype (get vars/*redis-types* kbase)
          klist (:list (get @vars/*redis-ops* ktype))
          val (klist k)]
      ;; (println "key:" k "kbase:" kbase "ktype:" ktype "list:" klist)
      ;; (println "key:" k "kbase:" kbase "ktype:" ktype "val:" val)
      (print "key:" k "isa" (if ktype ktype ":string") "=" val)
      )))

(defmethod junjible-db-action :list [args]
  (let [k (first (rest args))]
    (if (nil? k)
      (error-msg "missing key argument")
      (let [kbase (first (str/split k #":"))
            ;; debug1 (println "kbase=" kbase)
            ktype (get vars/*redis-types* kbase)
            ;; debug2 (println "ktype=" ktype " redis-ops=" @vars/*redis-ops*)
            klist (:list (get @vars/*redis-ops* ktype))
            ;; debug3 (println "klist=" klist)
            val (klist k)]
        (println val)))))

(defmethod junjible-db-action :del [args]
  (let [k (first (rest args))]
    (if (nil? k)
      (error-msg "missing key argument")
      (db/adel k))))

(defmethod junjible-db-action :srem [args]
  (let [k (nth args 1 nil)
        m (nth args 2 nil)]
    (if (nil? k)
      (error-msg "missing key argument")
      (if (nil? m)
        (error-msg "missing member argument")
        (do
          (db/srem k m)
          (println "removed:" m "from set:" k))))))

(defmethod junjible-db-action :hget [args]
  (let [k (nth args 1 nil)
        f (nth args 2 nil)]
    (if (nil? k)
      (error-msg "missing key argument")
      (if (nil? f)
        (error-msg "missing field argument")
        (db/hget k f)))))

(defmethod junjible-db-action :del-user [args]
  (let [user (nth args 1 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (db/del-user user))))

(defmethod junjible-db-action :add [args]
  (let [k (nth args 1 nil)
        v (nth args 2 nil)
        h (nth args 3 nil)
        members (rest (rest args))]
    (if (nil? k)
      (error-msg "missing key argument")
      (if (nil? v)
        (error-msg "missing value argument")
        (let [kbase (first (str/split k #":"))
              ktype (get vars/*redis-types* kbase)
              kadd (:add (get @vars/*redis-ops* ktype))]
          (if (= ktype :hash)
            (if (nil? h)
              (error-msg "missing hash value")
              (do
                (kadd k v h)
                (println "added:" k "{" v ":" h "}")))
            (if (= ktype :set)
              (do
                (kadd k members)
                (println "added:" k "=" members))
              (do
                (kadd k v)
                (println "added:" k "=" v)))))))))

(defmethod junjible-db-action :add-usergroup [args]
  (let [user (nth args 1 nil)
        group (nth args 2 nil)
        desc (nth args 3 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? group)
        (error-msg "missing group argument")
        (if (nil? desc)
          (error-msg "missing desc argument")
          (db/add-usergroup user group desc))))))

(defmethod junjible-db-action :get-usergroup [args]
  (let [user (nth args 1 nil)
        group (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? group)
        (error-msg "missing group argument")
        (println (db/get-usergroup user group))))))

(defmethod junjible-db-action :del-usergroup [args]
  (let [user (nth args 1 nil)
        group (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? group)
        (error-msg "missing group argument")
        (db/del-usergroup user group)))))

(defmethod junjible-db-action :set-usergroup [args]
  (let [user (nth args 1 nil)
        group (nth args 2 nil)
        members (rest (rest (rest args)))]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? group)
        (error-msg "missing group argument")
        (if (nil? members)
          (error-msg "missing members argument(s)")
          (db/set-usergroup user group members))))))

(defmethod junjible-db-action :update-usergroup [args]
  (let [user (nth args 1 nil)
        group (nth args 2 nil)
        members (rest (rest (rest args)))]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? group)
        (error-msg "missing group argument")
        (if (nil? members)
          (error-msg "missing members argument(s)")
          (db/update-usergroup user group members))))))

(defmethod junjible-db-action :add-userworld [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (db/add-userworld user world)))))

(defmethod junjible-db-action :del-userworld [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (db/del-userworld user world)))))

(defmethod junjible-db-action :get-userworld [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (println (db/get-userworld user world))))))

(defmethod junjible-db-action :update-userworld [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)
        wmap (rest (rest (rest args)))]
    (error-msg ":update-userworld:" args);
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (if (nil? wmap)
          (error-msg "no world settings")
          (let [worldmap (keywordize (apply hash-map wmap))]
            (println "worldmap:" worldmap);
            (db/update-userworld user world worldmap)))))))

(defmethod junjible-db-action :add-userinstance [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (db/add-userinstance user instance)))))

(defmethod junjible-db-action :del-userinstance [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (db/del-userinstance user instance)))))

(defmethod junjible-db-action :get-userinstance [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (println (db/get-userinstance user instance))))))

(defmethod junjible-db-action :update-userinstance [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)
        imap (rest (rest (rest args)))]
    ;; (verbose-msg ":update-userinstance:" args);
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (if (nil? imap)
          (error-msg "no instance settings")
          (let [instancemap (keywordize (apply hash-map imap))]
            ;; (verbose-msg "instancemap:" instancemap);
            (db/update-userinstance user instance instancemap)))))))

(defmethod junjible-db-action :undeploy-userinstance [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (db/undeploy-userinstance user instance)))))

;; -----------------

(defmethod junjible-db-action :get-plugin-settings [args]
  (let [plugin (nth args 1 nil)]
    (if (nil? plugin)
      (error-msg "missing plugin argument")
      (println (db/get-plugin-settings plugin)))))

(defmethod junjible-db-action :update-plugin-settings [args]
  (let [plugin (nth args 1 nil)
        kvs (rest (rest args))]
    (verbose-msg ":update-plugin-settings:" args);
    (if (nil? plugin)
      (error-msg "missing plugin argument")
      (if (nil? kvs)
        (error-msg "no plugin settings")
        (let [psmap (keywordize (apply hash-map kvs))]
          ;; (println "plugin settings map:" psmap) ;; DEBUG
          (println (db/update-plugin-settings plugin psmap (verbose?))))))))
;; turn on verbose-changes if we have verbose on

(defmethod junjible-db-action :del-plugin-settings [args]
  (let [plugin (nth args 1 nil)]
    (if (nil? plugin)
      (error-msg "missing plugin argument")
      (println (db/del-plugin-settings plugin)))))

;; -----------------

(defmethod junjible-db-action :get-plugin-user [args]
  (let [user (nth args 1 nil)
        plugin (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? plugin)
        (error-msg "missing plugin argument")
        (println (db/get-plugin-user user plugin))))))

(defmethod junjible-db-action :update-plugin-user [args]
  (let [user (nth args 1 nil)
        plugin (nth args 2 nil)
        kvs (rest (rest (rest args)))]
    (verbose-msg ":update-plugin-user:" args);
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? plugin)
        (error-msg "missing plugin argument")
        (if (nil? kvs)
          (error-msg "no plugin user settings")
          (let [pumap (keywordize (apply hash-map kvs))]
            (verbose-msg "pumap:" pumap);
            (println (db/update-plugin-user user plugin pumap (verbose?)))))))))
;; turn on verbose-changes if we have verbose on

(defmethod junjible-db-action :del-plugin-user [args]
  (let [user (nth args 1 nil)
        plugin (nth args 2 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? plugin)
        (error-msg "missing plugin argument")
        (println (db/del-plugin-user user plugin))))))

;; -----------------

(defmethod junjible-db-action :server [args]
  (let [user (nth args 1 nil)
        instance (nth args 2 nil)
        action (nth args 3 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? instance)
        (error-msg "missing instance argument")
        (if (nil? action)
          (error-msg "missing action argument")
          (println (db/junjible-server user instance action)))))))

(defmethod junjible-db-action :update-user [args]
  (let [user (nth args 1 nil)
        kvs (rest (rest args))]
    (verbose-msg ":update--user:" args);
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? kvs)
        (error-msg "no user values")
        (let [umap (keywordize (apply hash-map kvs))]
          (verbose-msg "umap:" umap);
          (db/update-user user umap))))))

(defmethod junjible-db-action :default [args]
  (verbose-msg "junjible-db: default:" args)
  (let [action (nth args 0 nil)]
    (if (nil? action)
      (if (= (db/ping) "PONG")
        (println "redis is alive")
        (println "redis is not responding"))
      (println "action unknown:" action))))

;; =================================

(defmethod junjible-db-action :add-permission [args]
  (let [perm (nth args 1 nil)
        special (nth args 2 nil)]
    (if (nil? perm)
      (error-msg "missing perm argument")
      (db/add-permission perm special))))

(defmethod junjible-db-action :del-permission [args]
  (let [perm (nth args 1 nil)]
    (if (nil? perm)
      (error-msg "missing perm argument")
      (db/del-permission perm))))

(defmethod junjible-db-action :get-permissions [args]
  (db/get-permissions))

(defmethod junjible-db-action :update-permissions [args]
  (let [perms (rest args)]
    (db/update-permissions perms)))

(defmethod junjible-db-action :update-categories [args]
  (let [categories (rest args)]
    (db/update-categories categories)))

(defmethod junjible-db-action :update-plugins [args]
  (let [plugins (rest args)]
    (db/update-plugins plugins)))

(defmethod junjible-db-action :update-pluginperms [args]
  (let [plugin (nth args 1 nil)
        perms (rest (rest args))]
    (if (nil? plugin)
      (error-msg "missing plugin argument")
      (if (empty? perms)
        (error-msg "no plugin values")
        (db/update-pluginperms plugin perms)))))

(defmethod junjible-db-action :activated-plugins [args]
  (let [user (nth args 1 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (db/activated-plugins user))))

(defmethod junjible-db-action :update-server [args]
  (let [server (nth args 1 nil)
        kvs (rest (rest args))]
    ;; (verbose-msg ":update-server:" args);
    (if (nil? server)
      (error-msg "missing server argument")
      (if (nil? kvs)
        (error-msg "no server values")
        (let [smap (keywordize (apply hash-map kvs))]
          ;; (verbose-msg "smap:" smap);
          (db/update-server server smap (verbose?)))))))

(defmethod junjible-db-action :del-server [args]
  (let [server (nth args 1 nil)]
    ;; (verbose-msg ":del-server:" args);
    (if (nil? server)
      (error-msg "missing server argument")
      (db/del-server server))))

(defmethod junjible-db-action :assign-instance [args]
  (let [user (nth args 1 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (db/assign-instance user))))

(defmethod junjible-db-action :unassign-instance [args]
  (let [user (nth args 1 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (db/unassign-instance user))))

(defmethod junjible-db-action :unassign-server [args]
  (let [server (nth args 1 nil)]
    (if (nil? server)
      (error-msg "missing server argument")
      (db/unassign-server server))))

(defmethod junjible-db-action :get-servers [args]
  (str-combine (db/get-servers)))

(defmethod junjible-db-action :servers-locked [args]
  (let [opts (rest args)]
    (apply db/servers-locked opts)))

(defmethod junjible-db-action :servers-unlocked [args]
  (db/servers-unlocked))

(defmethod junjible-db-action :servers-lockedp [args]
  (let [locked (db/servers-locked?)]
    (if locked locked "false")))

(defmethod junjible-db-action :servers-debug [args]
  (db/servers-locked :msg "servers-debug test of servers-locked"))

(defmethod junjible-db-action :get-userworld-field [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)
        field (nth args 3 nil)]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (if (nil? field)
          (error-msg "no field argument")
          (db/get-userworld-field user world field))))))

(defmethod junjible-db-action :set-userworld-field [args]
  (let [user (nth args 1 nil)
        world (nth args 2 nil)
        fields (rest (rest (rest args)))]
    (if (nil? user)
      (error-msg "missing user argument")
      (if (nil? world)
        (error-msg "missing world argument")
        (if (nil? fields)
          (error-msg "no field settings")
          (apply db/set-userworld-field (cons user (cons world fields))))))))

;; =========================================

(defn junjible-db [args]
  ;; REDIS
  (with-server (db/redis-options)
    (let [result (junjible-db-action args)]
      (if result
        (println result)))))
