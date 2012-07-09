(ns cavapanel.views.tests
  (:require [cavapanel.views.common :as common]
            [hiccup.page-helpers :as web]
            [noir.validation :as valid]
            [noir.session :as session]
            [noir.response :as response]
            [noir.cookies :as cookies]
            [future-contrib.java-utils :as utils]
            ;; [cavapanel.database :as db]
            )
  (:use [noir.core]
        [hiccup.core]
        [hiccup.page-helpers]
        [future-contrib.pphtml]
        [cavapanel.utils]
        ;; [redis.core :only [with-server]]
        [cavapanel.views.panelserver :only [debug-response]]))

(defn qunit-body [module & [params]]
  (let [name (if (list? module)
               (str-addcomma module)
               module)
        log?  (and params (is-true? (:log params))) ;; are we logging?
        title (str "Junjible unit-test: " name
                   (if log? "*" "")) ;; add a star if we are logging
        preamble (list
                  :head
                  [:title title]
                  [:meta {:charset "utf-8"}]
                  (include-css "/unit-tests/res/qunit.css")
                  (include-js "/js/jquery.min.js")
                  (include-js "/unit-tests/res/qunit.js")
                  (include-js "/js/junjible.js")
                  (include-js "/unit-tests/res/junjible-qunit.js"))
        modulejs (if (list? module)
                   (for [m module]
                     (include-js (str "/unit-tests/res/" m ".js")))
                   (list (include-js (str "/unit-tests/res/" module ".js"))))
        logjs (if log? (list (include-js "/unit-tests/res/junjible-log.js")))
        head (apply vector (merge-lists preamble modulejs logjs))]
    ;; (println "DEBUG params=" params " log?=" log? " title=" title)
    (pphtml-str (html5 head
                       [:body
                        [:h1#qunit-header title]
                        [:h2#qunit-banner]
                        [:div#qunit-testrunner-toolbar]
                        [:h2#qunit-userAgent]
                        [:h2#qunit-banner]
                        [:ol#qunit-tests]
                        [:div#qunit-fixture "test markup, will be hidden"]]))))

(defpage [:get "/unit-tests/smoketest"] {:as params}
  (qunit-body "smoketest" params))

(defpage [:get "/unit-tests/register"] {:as params}
  (qunit-body "register" params))
 
(defpage [:get "/unit-tests/login"] {:as params}
  (qunit-body "login" params))
 
(defpage [:get "/unit-tests/worlds"] {:as params}
  (qunit-body "worlds" params))

(defpage [:get "/unit-tests/groups"] {:as params}
  (qunit-body "groups" params))

(defpage [:get "/unit-tests/server"] {:as params}
  (qunit-body "server" params))

(defpage [:get "/unit-tests/admin-plugins"] {:as params}
  (qunit-body "admin-plugins" params))

(defpage [:get "/unit-tests/logout"] {:as params}
  (qunit-body "logout" params))

;; NOTE: by itself delete will not work because QUnit is not logged in
(defpage [:get "/unit-tests/delete"] {:as params}
  (qunit-body "delete" params))

(defpage [:get "/unit-tests/login_delete"] {:as params}
  (qunit-body (list "login" "delete") params))

(defpage [:get "/unit-tests/all"] {:as params}
  (qunit-body (list "smoketest" "register" "login"
                    ;; more tests go here
                    "worlds"
                    "groups"
                    ;; "server" doesn't work yet :(
                    ;; cannot logout else delete will not work
                    "delete") params))

;; (defn save-qunit-log [user log html results]
(defn save-qunit-log [log html results]
  (let [logdir (utils/relative-path-string "logs/unit-tests")
        logfile (str logdir "/" log ".html")]
    (mkdirp logdir)
    (utils/with-out-file logfile
      (println "<!-- QUnit result page")
      ;; (println (str "=user=" (if user user "") "="))
      (println (str "=log=" log "="))
      (doseq [k (keys results)]
             (println (str "=" (name k) "=" (get results k) "=")))
      (println "-->\n<html>")
      (println html)
      (println "</html>"))
    (str "wrote " logfile)))

;; REDIS
;; DO NOT REQUIRE USER
(defpage [:post "/unit-tests/log"] {:as logData}
  (let [;; user (db/user?)
        log (:log logData)
        html (:html logData)
        results (:results logData)]
    (if (verbose?)
      ;; (println "/unit-tests/log for" user "=" results))
      (println "/unit-tests/log =" results))
    ;; (let [msg (save-qunit-log user log html results)
    (let [msg (save-qunit-log log html results)
          result (not (.startsWith msg "ERROR"))]
      (response/json (debug-response { :success result, :msg msg})))))
