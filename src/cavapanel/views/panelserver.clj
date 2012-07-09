(ns cavapanel.views.panelserver
  (:require [clojure.string :as str]
            [noir.response :as response]
            [noir.session :as session] ;; only for pre-route
            [hiccup.page-helpers :as web]
            [clj-json.core :as json]
	    [cavapanel.vars :as vars]
	    [cavapanel.views.common :as common]
            [cavapanel.database :as db])
  (:use [noir.core] ;; non defpage only for pre-route
        [clojure.set]
        [future-contrib.java-utils :only [as-int]]
        [hiccup.core]
        [redis.core :only [with-server]]
        [cavapanel.utils]
        [clojure.java.shell :only [sh]]))

(defn please-login []
  (hash-map :success false
            :msg "user not authenticated: please login"))

(defn debug-response [response]
  (do
    ;; (if (verbose?)
    ;;   (println "response to " (db/user?) ":" response))
    response))

                                        ;
                                        ; UNIT TEST LOG HANDLER
                                        ;
(defpage "/unit-test/:pass/:fail"
  {:keys [pass fail]}
  []
  (println
   (str "==============\n"
        "Unit test results (pass/fail) => "
        pass ":" fail))
  (response/redirect "/"))
                                        ;
                                        ; SERVER ROUTES BEGIN HERE
                                        ;
(pre-route "/server/*" {}
           (with-server (db/redis-options)
             (if-not (db/user?)
               (do
                 (session/flash-put! "/panel")
                 (response/redirect "/#login")))))

(defpage "/server/intro" []
  (html [:p "Download the Junjible launcher" 
      [:a {:class "uline" :href "https://junjible.com/download/JunjibleLauncher.jar"} " here. "]
      [:span#info-download.infotip "&nbsp;"]]
[:div.tooltip
    [:div 
[:p "After you’ve completed the download, simply launch the program! (Mac users: Double click. Windows: Right Click> Run.)"]
[:p "You are now running Junjible!"]

[:p "Now you can use the panel to communicate with your server. Click \"Server Info\" under the first tab to start/stop/restart your server.  Your ip and port will also be listed here."]

[:p "Should ANY troubles arise, bugs, misunderstandings, etc., please click 'support' at the top of the panel."]

[:p "Enjoy the beta!"]]]
       [:script {:type "text/javascript"}
        (str "
      $('#info-download').tooltip({ position: 'top left', effect: 'slide',offset: [70, 200]});

")]

))

(defpage "/server/manage" []
  (html 
   ;;[:div.threecol
   ;;[:a {:class "panelControl newInstance", :href "#"} "Add New Instance"]
   ;;         [:div.server-pane
   ;;          [:ul.list.servers
   ;;           [:li.initial "Loading instances..."]
   ;;           ]
   ;;          ]
   ;;         ]
   [:div.twocol
    [:a.panelControl.startServer {:href "#"} "start"]
    [:a {:class "panelControl stopServer", :href "#"} "stop"]
    [:a {:class "panelControl restartServer", :href "#"} "restart"]
    ]
   [:div.fourcol.panelReadout
    [:span#server-readout

     [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3", :style "width:300px;"}

      [:tr
       [:td {:style "text-align:left;width:40px;"} "Host:"] 
       [:td#hostname {:style "text-align:left;"} ""] 
       ]
      [:tr
       [:td {:style "text-align:left;width:40px;"} "Port:"] 
       [:td#portname {:style "text-align:left;"} ""] 
       ]
      [:tr
       [:td#servermsg {:colspan "2", :style "text-align:left;"}]

       ]
      ]
     ]
    (web/include-js "/js/instances.js")
    ]
   [:div.sixcol.panelReadout.last
    [:span#setworld.panelReadout "test"]
    [:span [:div#top-right-indicator.yield]] ;; show yellow until status updated
    ]
   ))

;; NOTE: server will be deprecated in favor of instance
(defpage [:post "/server/control"] {:keys [command instance] :as opts}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/control: command:" command "instance:" instance "user:" user)
          (let [result (db/junjible-server user instance command)]
            (verbose-msg "  result=" result)
            (response/json (debug-response result))))))))

;; first name is group name, subsequent names are members
(defn jgroup [groupname members]
  (hash-map :groupname groupname
            :groupmembers (apply vector
                                 (for [m members] {:name m}))))

(defn jgroup1 []
  (jgroup "warriors"
          (list "flunky" "blunky" "basher" "crasher")))

(defn jgroup2 []
  (jgroup "baseball furies"
          (list "tex" "wrex" "flex" "bob" "germaine")))

(defn jgroup3 []
  (jgroup "orphans"
          (list "swizzle" "bizzle" "schnizzle" "wurble" "glerbil" "glorp")))

(defn jgroups1 []
  (hash-map :groups (vector (jgroup1) (jgroup2) (jgroup3))))

(defn jplayers [players]
  (hash-map :players (apply vector players)))

(defn jplayers1 []
  (jplayers (list "flunky" "blunky" "basher" "crasher" "tex" "wrex" "flex" "bob" "germaine" "swizzle" "bizzle" "schnizzle" "wurble" "glerbil" "glorp" "extra1" "extra2")))
;;(response/json (merge (jgroups1) (jplayers1))))

(defpage "/server/get-groups" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 21) ;; DEBUG
          user (db/user?)
          groupskey (str "groups:" user)
          groups (sort (for [k (keys (db/hgetall groupskey))] (name k)))
          jsongroups (hash-map :groups (apply vector (for [g groups] (jgroup g (db/get-usergroup user g)))))
          jsonplayers (jplayers (sort (apply union (for [g groups] (db/get-usergroup user g)))))
          groupdata (merge jsongroups jsonplayers)]
      (if (verbose?)
        (do
          ;; (println "/server/get-groups user:" user "groupskey:" groupskey)
          ;; (println "sending:" groupdata "user:" user) ; DEBUG
          (flush)))
      (response/json groupdata))))

(defn set-save-groups [sg]
  ;;(println "set-save-groups BEFORE:" @vars/*save-groups*)
  (swap! vars/*save-groups* use-new sg)
  ;;(println "set-save-groups AFTER :" @vars/*save-groups*)
  )

;; (defpage [:post "/server/save-groups"] {:keys [groupList]}
(defpage [:post "/server/save-groups"] {:as groupList}
  (with-server (db/redis-options)
    (let [user (db/user?)
          groupskey (str "groups:" user)
          oldgroups (set (for [k (keys (db/hgetall groupskey))] (name k)))]
      ;; (verbose-msg "/server/save-groups for" user "=" groupList)
      ;; (verbose-msg "/server/save-groups for" user)
      (set-save-groups nil)
      (set-result-msg "")
      (if (nil? user)
        (do
          (println "/server/save-groups got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))

        (let [players (:players groupList)
              groups (:groups groupList)]
          ;; (if players
          ;;   (doseq [player players]
          ;;     (println "player:" player)))
          (if groups
            (do
              (doseq [group groups]
                (let [gmap (nth group 1)
                      groupname (:groupname gmap)
                      groupmembers (set (for [m (vals (:groupmembers gmap))] (:name m)))]
                  ;; (println "groupname:" groupname)
                  (set-save-groups (cons groupname @vars/*save-groups*))
                  ;; (doseq [member groupmembers]
                  ;;   (println "  member:   " member))
                  (add-result-msg (db/update-usergroup user groupname groupmembers))))
              ;; (println "oldgroups:" oldgroups)
              (let [newgroups (set @vars/*save-groups*)
                    delgroups (difference oldgroups newgroups)]
                ;; (println "newgroups:" newgroups)
                ;; (println "delgroups:" delgroups)
                (doseq [g delgroups]
                  (add-result-msg (db/del-usergroup user g))))))
          (response/json (debug-response {:success true, :msg (result-msg)})))))))

(defpage "/server/get-categories" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          categories (db/get-categories)
          jsoncategories (hash-map :categories
                                   (apply vector (for [c categories] c)))]
      ;; (verbose-msg "/server/get-categories user:" user)
      ;; (verbose-msg "sending:" jsoncategories) ;; DEBUG
      (response/json jsoncategories))))

(defpage [:post "/server/save-categories"] {:as categoriesData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (verbose-msg "/server/save-categories for" user "=" categoriesData)
      (set-result-msg "")
      (if (nil? user)
        (do
          (println "/server/save-categories got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [categories (set (:categories categoriesData))]
          (if categories
            (do
              (verbose-msg "updating=" categories)
              (add-result-msg (db/update-categories categories))))
          (response/json (debug-response { :success true, :msg (result-msg) })))))))

(defpage "/server/get-worlds" []
  (with-server (db/redis-options)
    ;; (verbose-msg "/server/get-worlds")
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)]
      (verbose-msg "/server/get-worlds user:" user)
      (if (nil? user)
        (do
          (println "/server/get-worlds got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [worldmaps (db/get-userworlds user true)
              jsonworlds (hash-map :worlds (apply vector worldmaps))]
          ;; (verbose-msg "  sending:" jsonworlds) ;; DEBUG
          (response/json (debug-response jsonworlds)))))))

(defn set-save-worlds [sg]
  ;;(println "set-save-worlds BEFORE:" @vars/*save-worlds*)
  (swap! vars/*save-worlds* use-new sg)
  ;;(println "set-save-worlds AFTER :" @vars/*save-worlds*)
  )

(defpage [:post "/server/save-worlds"] {:as worldData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (verbose-msg "/server/save-worlds for" user)
      (if (nil? user)
        (do
          (println "/server/save-worlds got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [oldworlds (db/get-userworlds-set user)
              worlds (:worlds worldData)]
          ;; (verbose-msg "/server/save-worlds for" user "=" worldData)
          (set-save-worlds nil)
          (set-result-msg "")
          (if worlds
            (do
              (doseq [world worlds]
                (let [worldmapname (nth world 1)
                      name (:name worldmapname)
                      worldmap (dissoc worldmapname :name :id)]
                  (set-save-worlds (cons name @vars/*save-worlds*))
                  (add-result-msg (db/update-userworld user name worldmap))))
              (let [newworlds (set @vars/*save-worlds*)
                    delworlds (difference oldworlds newworlds)]
                ;; (verbose-set "  newworlds" newworlds)
                ;; (verbose-set "  oldworlds" oldworlds)
                ;; (verbose-set "  delworlds" delworlds)
                (if (not-empty delworlds)
                  (doseq [w delworlds]
                    (add-result-msg (db/del-userworld user w))))
                (response/json (debug-response { :success true, :msg (result-msg) }))))))))))

(defn set-save-instances [si]
  ;;(println "set-save-instances BEFORE:" @vars/*save-instances*)
  (swap! vars/*save-instances* use-new si)
  ;;(println "set-save-instances AFTER :" @vars/*save-instances*)
  )

(defpage [:post "/server/save-instances"] {:as instanceData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (verbose-msg "/server/save-instances" instanceData "user:" user)
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [oldinstances (db/get-userinstances-set user)
              instances (:instances instanceData)]
          (set-save-instances nil)
          (set-result-msg "")
          (binding [vars/*success* (atom true)]
            (if instances
              (do
                (doseq [instance instances]
                  ;; (println "instance=>" instance "<=")
                  (let [instancemapname (nth instance 1)
                        name (:name instancemapname)
                        instancemap (dissoc instancemapname :name)]
                    ;; (verbose-msg "name:" name "instancemap:" instancemap)
                    (set-save-instances (cons name @vars/*save-instances*))
                    (let [updated (db/update-userinstance user name instancemap)]
                      (if (.startsWith updated "error:")
                        (set-success false)) ;; return FAIL
                      (add-result-msg updated))))
                (let [newinstances (set @vars/*save-instances*)
                      delinstances (difference oldinstances newinstances)]
                  ;; (verbose-set "  newinstances" newinstances)
                  ;; (verbose-set "  oldinstances" oldinstances)
                  ;; (verbose-set "  delinstances" delinstances)
                  (if (not-empty delinstances)
                    (doseq [w delinstances]
                      (add-result-msg (db/del-userinstance user w))))
                  (response/json (debug-response
                                  (hash-map :success (success?)
                                            :msg (result-msg)))))))))))))

(defn jinstance [user instance]
  (let [instmap (db/get-userinstance user instance true)
        changes (:changes instmap)
        msg (:msg instmap)
        msg1 (if changes (str msg " -- changes pending --") msg)
        instmap1 (merge instmap (hash-map :name instance
                                          :msg msg1))]
    instmap1))

(defpage "/server/get-instances" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 43) ;; DEBUG
          user (db/user?)
          instances (db/get-userinstances-set user)
          jsoninstances (hash-map :instances
                                  (apply vector
                                         (for [i instances]
                                           (jinstance user i))))]
      (verbose-msg "/server/get-instances user:" user)
      ;;              "jsoninstances=" jsoninstances)
      (response/json (debug-response jsoninstances)))))

(defn set-save-instances [si]
  ;;(println "set-save-instances BEFORE:" @vars/*save-instances*)
  (swap! vars/*save-instances* use-new si)
  ;;(println "set-save-instances AFTER :" @vars/*save-instances*)
  )

(defn juser [user usermap]
  (merge {:name user} (dissoc usermap :pass)))
;;  (merge {:name user} (dissoc usermap :pass :admin :active :level :usage)))

(defpage "/server/get-user" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 54) ;; DEBUG
          user (db/user?)]
      (if (nil? user)
        (do
          (println "/server/get-user got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [usermap (db/get-user user)
              jsonuser (hash-map :user (juser user usermap))]
          ;; (verbose-msg "/server/get-user user:" user)
          (response/json (debug-response jsonuser)))))))
;;        (response/json jsonuser)))))

(defpage [:post "/server/save-user" ] {:as userData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (if (nil? user)
        (do
          (println "/server/save-user got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [oldusermap (db/get-user user)
              oldpassword (:pass oldusermap)
              admin (:admin oldusermap)
              active (:active oldusermap)
              level (:level oldusermap)
              usage (:usage oldusermap)
              userEdit (:user userData)
              email (:email userEdit)
              minecraft (:minecraft userEdit)
              password (:password userEdit)
              newpassword (:newpassword userEdit)
              newpassword2 (:newpassword2 userEdit)
              nochangepw (and (empty? newpassword) (empty? newpassword2)) ]
          (verbose-msg "/server/save-user" userData "user:" user)
          (if (not= password oldpassword)
            (do
              (verbose-msg "password given =" password
                           "=DOES NOT match=" (:pass oldusermap)
                           "= for user: " user)
              (response/json
               (debug-response { :saved false, :error "password does not match" })))
            (let [problem (if (not nochangepw)
                            (if (not= newpassword newpassword2)
                              (response/json
                               (debug-response { :saved false, :error "new passwords do not match" }))
                              (if (< (count newpassword) 6)
                                (response/json
                                 (debug-response { :saved false, :error "new password too short (must be at least 6 characters)" })))))]
              (if problem
                problem
                (if (is-true? (:delete-account userEdit))
                  (do
                    (verbose-msg "password match, deleting account for:" user)
                    (db/del-user user)
                    (response/json (debug-response { :saved true })))
                  (do
                    (verbose-msg "password match, updating" user)
                    (db/update-user user {:pass (if nochangepw oldpassword newpassword)
                                          :email email :minecraft minecraft
                                          :admin admin :active active
                                          :level level :usage usage})
                    (response/json (debug-response { :saved true }))))))))))))

;; create a JSON vector for names, where
;; v is the value to assign to the vector
(defn jsetvector [names v]
  (apply vector (for [name names] (vector name v))))

;; create a JSON vector for names, where
;; the value is false unless the name is present in myset
(defn jsetvector-in-set [names myset]
  (apply vector (for [k names]
                  (let [n (name k)]
                    (vector n (contains? myset n))))))

(defn get-boolean-keys [hm pred]
  (set (remove-nils (for [key (keys hm)] (if (= (get hm key) pred) key)))))

;; (defn jplugin-perm [user group member plugin]
;;   (let [pkey (str "plugin_perms:" plugin)
;;         pperms (smembers pkey)]
;;     (if (not (empty? pperms))
;;       (hash-map :name plugin :plugperms (jsetvector pperms true)))))

;; (defn jplugin-perms [user group member]
;;   (let [plugins (db/get-plugins)]
;;     (apply vector (remove-nils (for [plugin plugins]
;;                                  (jplugin-perm user group member plugin))))))

;; needs to return
;; {:name superplug, :plugperms [[splash true] [take_a_bath true] [splish true]]}
(defn jplugin-perm [user plugin myset]
  (let [;; REDIS debug (Thread/sleep 10) ;; DEBUG
        psmap (db/get-plugin-user user plugin)
        pname (:name psmap)
        available (:available psmap)
        ;; activated (and available (:activated psmap))
        ;; NOTE: use only available for now... because otherwise the GUI
        ;; has to be manually refreshed after plugins are activated
        pperms (if available (db/get-pluginperms plugin))]
    (if (not (empty? pperms))
      (hash-map :name pname :plugperms (jsetvector-in-set pperms myset)))))

;; needs to return
;; [{:name superplug, :plugperms [[splash true] [take_a_bath true] [splish true]]}
;;  {:name wonderplug, :plugperms [[happiness true] [liberty true] [life true]]}
;;  {:name nastyplug, :plugperms [[hyperventilate true] [itch true] [bleed true]]}]
(defn jplugin-perms [user myset]
  (let [plugins (db/get-plugins)]
    (apply vector
           (remove-nils
            (for [plugin plugins]
              (jplugin-perm user plugin myset))))))

(defn jpermissions [user group member]
  (let [perms (db/get-permissions)
        specialperms (get-boolean-keys perms true)
        basicperms (get-boolean-keys perms false)
        ugperms (db/get-userpermissions user group member)
        special (jsetvector-in-set specialperms ugperms)
        basic (jsetvector-in-set basicperms ugperms)
        plugin (jplugin-perms user ugperms)]
    ;; DEBUG
    ;; (println "ugperms=" ugperms)
    (hash-map :group group :member member
              :special special
              :basic basic
              :plugin plugin)))

(defpage [:post "/server/get-permissions"] {:keys [group member] :as opts}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (if (nil? user)
        (do
          (println "/server/get-permissions got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          ;; (verbose-msg "/server/get-permissions: group=" group "member=" member "user:" user "opts:" opts)
          (let [;; REDIS debug (Thread/sleep 10) ;; DEBUG
                jsonperms (jpermissions user group member)]
            (response/json (debug-response jsonperms))))))))

(defn parse-plugin-perms [plugins]
  (apply merge (for [plugin plugins] (hash-map
                                      (keyword (:name plugin))
                                      (apply merge (for [v (vals (:plugperms plugin))]
                                                     (hash-map
                                                      (keyword (nth v 0))
                                                      (is-true-str? (nth v 1)))))))))

(defpage [:post "/server/save-permissions"] {:as permList}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (verbose-msg "/server/save-permissions for" user "=" permList)
      ;; (verbose-msg "/server/save-permissions for" user)
      (set-result-msg "")
      (if (nil? user)
        (do
          (verbose-msg "/server/save-permissions got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))

        (let [group (:group permList)
              member (:member permList)
              ;;            basic (for [perm-bool (vals (:basic permList))] (hash-map (keyword perm-bool) perm-bool))
              special (apply merge (for [v (vals (:special permList))] (hash-map (keyword (nth v 0)) (is-true-str? (nth v 1)))))
              basic (apply merge (for [v (vals (:basic permList))] (hash-map (keyword (nth v 0)) (is-true-str? (nth v 1)))))
              plugin (parse-plugin-perms (vals (:plugin permList)))]
          ;; DEBUG
          ;; (verbose-msg "group=" group "member=" member "special=" special "basic=" basic "plugin=" plugin)
          (add-result-msg (db/json-userpermissions user group member special basic plugin))
          ;; consider testing success by looking for ERROR at beginning of msg
          (response/json (debug-response {:success true, :msg (result-msg)})))))))

;; -----------------------------------------------

;; returns just the number of plugins
(defpage "/server/get-plugins-count" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          plugins (db/get-plugins)
          n (count plugins)]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          ;; (verbose-msg "/server/get-plugins-count user:" user)
          (response/json (debug-response n)))))))

(defn get-plugins-page [plugins & [page]]
  (with-server (db/redis-options)
    (let [n (count plugins)
          j (if (nil? page) 0 (* page 10))
          k (if (nil? page) n (min n (* (inc page) 10)))]
      (for [i (range j k)]
        (let [plugin (nth plugins i)
              psmap (db/get-plugin-settings plugin)
              psmap1 (merge psmap (hash-map :plugin plugin))] ;; must add id
          psmap1)))))

;; get all the plugins
(defpage [:get "/server/get-plugins"] []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          plugins (apply list (db/get-plugins)) ;; MUST convert to list for nth
          jsonplugins (hash-map :plugins (apply vector (get-plugins-page plugins)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-plugins user:" user)
          (response/json (debug-response jsonplugins)))))))

;; get one page of plugins: { "page": 1 }
(defpage [:post "/server/get-plugins"] {:as pageData}
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          plugins (apply list (db/get-plugins)) ;; MUST convert to list for nth
          pagedata (:page pageData)
          page (if pagedata (as-int pagedata) 0)
          jsonplugins (hash-map :plugins (apply vector (get-plugins-page plugins page)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-plugins page: " page ", user:" user)
          (response/json (debug-response jsonplugins)))))))

(defpage [:post "/server/save-plugins"] {:as pluginData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      ;; (verbose-msg "/server/save-plugins for" user "=" pluginData)
      (set-result-msg "")
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [plugins (:plugins pluginData)]
          (if plugins
            (do
              (doseq [p plugins]
                (let [psmap (nth p 1) ;; skip sequence number
                      ;; debug (verbose-msg "DEBUG save-plugins psmap=" psmap)
                      settings (:settings psmap) ;; remove array indexes
                      plugin (:plugin psmap) ;; get id
                      settings-vals (vals settings)
                      settings1 (apply vector settings-vals)
                      psmap1 (merge (dissoc psmap :plugin)
                                    (hash-map :settings settings1))]
                  ;; (verbose-msg "save-plugins psmap1=" psmap1)
                  ;; FIXME check for errors here
                  (if plugin
                    (verbose-msg (db/update-plugin-settings plugin psmap1 true)))
                  ))
              (add-result-msg "plugins saved")))
          (response/json (debug-response { :success true, :msg (result-msg) })))))))

;; returns just the number of AVAILABLE user plugins
(defpage "/server/get-userplugins-count" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [n (db/get-userplugins-count)]
          (verbose-msg "/server/get-userplugins-count user:" user)
          (response/json (debug-response n)))))))

(defpage [:get "/server/get-userplugins"] []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          ;; debug2 (println "/server/get-userplugins user:" user) ;; DEBUG
          plugins (apply list (db/get-plugins)) ;; MUST convert to list for nth
          jsonplugins (hash-map :plugins (apply vector (db/get-userplugins-page user plugins)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-userplugins user:" user)
          (response/json (debug-response jsonplugins)))))))

;; get one page of userplugins: { "page": 1 }
(defpage [:post "/server/get-userplugins"] {:as pageData}
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          plugins (apply list (db/get-plugins)) ;; MUST convert to list for nth
          pagedata (:page pageData)
          page (if pagedata (as-int pagedata) 0)
          jsonplugins (hash-map :plugins (apply vector (db/get-userplugins-page user plugins page)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-userplugins page: " page ", user:" user)
          (response/json (debug-response jsonplugins)))))))

(defpage [:post "/server/save-userplugins"] {:as pluginData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      ;; (verbose-msg "/server/save-userplugins for" user "=" pluginData)
      (set-result-msg "")
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [plugins (:plugins pluginData)]
          (if plugins
            (do
              (doseq [p plugins]
                (let [pumap (nth p 1) ;; skip sequence number
                      settings (:settings pumap) ;; remove array indexes
                      plugin (:plugin pumap) ;; get id
                      settings-vals (vals settings)
                      settings1 (apply vector settings-vals)
                      pumap1 (merge (dissoc pumap :plugin)
                                    (hash-map :settings settings1))]
                  ;; (verbose-msg "save-userplugins pumap1=" pumap1)
                  ;; FIXME check for errors here
                  (verbose-msg (db/update-plugin-user user plugin pumap1 true))
                  ))
              (add-result-msg "user plugin configuration saved")))
          (response/json (debug-response { :success true, :msg (result-msg) })))))))

;; ----------------------------
;; new format for users, displayUsers(data) in admin.js expects:
;; {"users":[
;; {"minecraft":"fred",
;; "username":"fred",
;; "registered":1337115734769,
;; "email":"fred@nowhere.net",
;; "type":true,
;; "status":"paid",  (see ticket #265)
;; "suspended":false,
;; "level":"10", (can be 5, 10, 15, or 20)
;; "usage":"string goes here",
;; }
;; ]}
;;
(defn jusers1 [users & [page]]
  (let [n (count users)
        j (if (nil? page) 0 (* page 10))
        k (if (nil? page) n (min n (* (inc page) 10)))]
    (for [i (range j k)]
      (let [user (nth users i)
            umap (db/get-user user)
            umap1 (dissoc umap :pass)
            umap2 (merge umap1 (hash-map :username user))]
        umap2))))

;; get all the users
(defpage [:get "/server/get-users"] []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          users (apply list (db/get-users)) ;; MUST convert to list for nth
          debug (verbose-msg "get-users users=" users)
          jsonusers (hash-map :users (apply vector (jusers1 users)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-users user:" user)
          (verbose-msg "sending:" jsonusers) ;; DEBUG
          (response/json jsonusers))))))

;; get one page of users: { "page": 1 }
(defpage [:post "/server/get-users"] {:as pageData}
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          users (apply list (db/get-users)) ;; MUST convert to list for nth
          pagedata (:page pageData)
          page (if pagedata (as-int pagedata) 0)
          jsonusers (hash-map :users (apply vector (jusers1 users page)))]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          (verbose-msg "/server/get-users page: " page ", user:" user)
          (verbose-msg "sending:" jsonusers) ;; DEBUG
          (response/json jsonusers))))))

;; returns just the number of users
(defpage "/server/get-users-count" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 39) ;; DEBUG
          user (db/user?)
          users (db/get-users)
          n (count users)]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (do
          ;; (verbose-msg "/server/get-users-count user:" user)
          ;; (verbose-msg "sending:" n) ;; DEBUG
          (response/json n))))))

(defpage [:post "/server/save-users" ] {:as userData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      (if (nil? user)
        (do
          (error-msg "/server/get-plugins got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [users (:users userData)]
          (verbose-msg "/server/save-users for" user "=" userData)
          (set-result-msg "")
          (if users
            (do
              (doseq [u users]
                (let [umap (nth u 1) ;; skip sequence number
                      username (:username umap)
                      oldumap (db/get-user username)
                      password (:pass  oldumap)
                      umap1 (dissoc umap :username)
                      umap2 (merge umap1 (hash-map :pass password))]
                  (verbose-msg "umap2=" username "=" umap2)
                  (add-result-msg (db/update-user username umap2))))
              (response/json (debug-response { :success true, :msg (result-msg) })))))))))

(defn jdash [i]
  (hash-map (str "Statistic #" i) (* 100 i)))

(defn jdashboard []
  (apply vector (for [i (range 10)] (jdash i))))

(defpage "/server/get-dashboard" []
  (with-server (db/redis-options)
    (let [;; REDIS debug (Thread/sleep 11) ;; DEBUG
          jsondash (hash-map :dashboard (jdashboard))]
      (verbose-msg "/server/get-dashboard:")
      (verbose-msg "sending:" jsondash) ;; DEBUG
      (response/json (debug-response jsondash)))))

(defpage "/server/get-images" []
  (let
      [output (second (second (sh "jar"
                                  "tvf"
                                  "cavapanel-0.1.0-tmarble1.jar")))
       lines (clojure.string/split output #"\s")
       png-images (filter
                   #(if (>= (count %) 3)
                      (= "png" (subs % (- (count %) 3))))
                   lines)
       jpg-images (filter
                   #(if (>= (count %) 3)
                      (= "jpg" (subs % (- (count %) 3))))
                   lines)
       images (reduce conj png-images jpg-images)]
    (response/json images) ))

;; payment controllers

;; This is the initial page for selecting a plan and entering in cc info.
(defpage "/server/payments" []
     (html 
            (web/include-js "/js/countries2.js")
               [:div#payments
                 [:div#payment-msg {:style "display:none;"}]
                 [:div#payment-status]
                   [:form {:onsubmit "return false"}
                  [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
                   [:tr
                    [:td "Select Your Plan:"]
                    [:td.left 
[:input {:type "radio" :name "plan" :value "plan 1" :checked "checked"}][:span " 5 users @ $10/mo.  "][:br]
[:input {:type "radio" :name "plan" :value "plan 2"}][:span " 10 users @ $15/mo.  "][:br]
[:input {:type "radio" :name "plan" :value "plan 3"}][:span " 15 users @ $20/mo.  "][:br]
[:input {:type "radio" :name "plan" :value "plan 4"}][:span " 20 users @ $25/mo.  "]
                    ]
                    [:td]
                    ]
                  [:tr
                    [:td]
                    [:td.left (web/image "/images/cards.jpg" "")]
                    [:td]
                  ]
                   [:tr
                    [:td "Credit Card Number:"]
                    [:td.left [:input {:type "text", :name "ccnum",
                                  :maxlength "30", :value ""}]]
                    
                    ]
                   [:tr
                    [:td "Expiration Date:"]
                    [:td.left [:input {:type "text", :name "expmon",
                                  :maxlength "2", :size "2", :value "mm"}]
                         [:span " / "]
                         [:input {:type "text", :name "expyear",
                                  :maxlength "2", :size "2", :value "yy"}]
                    [:span " CVC: "][:input {:type "text", :name "cvc",
                                  :maxlength "3", :size "3", :value ""}]
                    ]
                    [:td]
                    ]
                   [:tr
                    [:td "Cardholder Name:"]
                    [:td.left [:input {:type "text", :name "ccname",
                                  :maxlength "40", :value ""}]]
                    [:td]
                    ]
                   [:tr
                    [:td "Address 1:"]
                    [:td.left [:input {:type "text", :name "ccaddy1",
                                  :maxlength "100", :value ""}]]
                    [:td]
                    ]
                   [:tr
                    [:td "Address 2:"]
                    [:td.left [:input {:type "text", :name "ccaddy2",
                                  :maxlength "100", :value ""}]]
                    [:td]
                    ]
                   [:tr
                    [:td "City:"]
                    [:td.left [:input {:type "text", :name "city",
                                  :maxlength "25", :value ""}]]
                    [:td]
                    ]
                   [:tr
                    [:td "Country:"]
                    [:td.left [:select {:onchange "print_state('state',this.selectedIndex);", 
                                   :id "country" 
                                   :name "country",
                                  }]
                    ]
                    [:td]
                    ]
                   [:tr
                    [:td "State / Province:"]
                    [:td.left [:select {:id "state" 
                                   :name "state",
                                  }]
     [:script {:type "text/javascript"}
        (str "
      print_country(\"country\");
      ")]
                    ]
                    [:td]

                    ]
                   [:tr
                    [:td "Zip / Postal Code:"]
                    [:td.left [:input {:type "text", :name "zip",
                                  :maxlength "20", :value ""}]]
                    [:td]
                    ]
                   [:tr
                    [:td {:colspan "2", :align "right"}
                     [:input#payment-submit {:type "button", :value "Process Payment"}]
                     ]
                    ]
                   [:tr
                    [:td {:colspan "2", :align "left"}]
                    ]
                   ]
                  ] ;; form
               ] ;; #payments
            (web/include-js "/js/payments.js")

     )
)

(defpage "/server/get-userpayments" []
(str "
{
\"status\":\"Account in good standing\",
\"plan\":\"plan 2\",
\"ccnum\":\"4657\",
\"expmon\":\"06\",
\"expyear\":\"14\",
\"ccname\":\"Horatio McHornswoggle\",
\"ccaddy1\":\"123 AnyStreet\",
\"ccaddy2\":\"apt. 123\",
\"city\":\"14\",
\"state\":\"Maine\",
\"country\":\"USA\",
\"zip\":\"12345\"
}
")
  
)

(defpage [:post "/server/submit-payment"] {:as paymentData}
  (with-server (db/redis-options)
    (let [user (db/user?)]
      ;; (verbose-msg "/server/save-userplugins for" user "=" pluginData)
      (set-result-msg "")
      (if (nil? user)
        (do
          (error-msg "/server/submit-payment got a nil user - pre-route failed.")
          (response/json (debug-response (please-login))))
        (let [payment (:payment paymentData)]
          (if payment
            (do
              (add-result-msg "user payment details submitted")))
          (response/json (debug-response { :success true, :msg "user payment details submitted" })))))))










