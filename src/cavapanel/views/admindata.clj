(ns cavapanel.views.admindata
  (:require [clojure.string :as str]
            [noir.response :as response]
            [noir.session :as session] ;; only for pre-route
            [hiccup.page-helpers :as web]
            [clj-json.core :as json]
	    [cavapanel.views.common :as common]
            [cavapanel.database :as db])
  (:use [noir.core] ;; non defpage only for pre-route
        [clojure.set]
        [future-contrib.java-utils :only [as-int]]
        [hiccup.core]
        [redis.core :only [with-server]]
        [cavapanel.utils]))

(defn debug-response [response]
  (do
    (if (verbose?)
      (println "response to " (db/user?) ":" response))
    response))

(pre-route "/admin/*" {}
           (with-server (db/redis-options)
             (let [user (db/user?)
                   admin? (and user (db/admin-user? user))]
               (if-not admin?
                 (if user
                   (response/redirect "/panel")
                   (do ;; not even logged in
                     (session/flash-put! "/panel")
                     (response/redirect "/#login")))))))

(defpage "/admin/data/getusers/0" []
  (str "
{\"users\":[{\"id\":1,\"name\":\"Naida Howard\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Theodore Atkins\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Blythe Baldwin\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Todd Gomez\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Abigail Hansen\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":false},
{\"id\":1,\"name\":\"Danielle Mejia\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Zane Harrison\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Justina Kane\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Ethan Curry\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"Kato Lindsay\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true}]}
"))
(defpage "/admin/data/getusers/1" []
  (str "
{\"users\":[{\"id\":1,\"name\":\"2Naida Howard\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Theodore Atkins\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Blythe Baldwin\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Todd Gomez\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Abigail Hansen\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":false},
{\"id\":1,\"name\":\"2Danielle Mejia\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Zane Harrison\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Justina Kane\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Ethan Curry\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"2Kato Lindsay\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true}]}
"))
(defpage "/admin/data/getusers/2" []
  (str "
{\"users\":[{\"id\":1,\"name\":\"3Naida Howard\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Theodore Atkins\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Blythe Baldwin\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Todd Gomez\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Abigail Hansen\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":false},
{\"id\":1,\"name\":\"3Danielle Mejia\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Zane Harrison\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Justina Kane\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Ethan Curry\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"3Kato Lindsay\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true}]}
"))
(defpage "/admin/data/getusers/3" []
  (str "
{\"users\":[{\"id\":1,\"name\":\"4Naida Howard\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Theodore Atkins\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Blythe Baldwin\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Todd Gomez\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Abigail Hansen\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":false},
{\"id\":1,\"name\":\"4Danielle Mejia\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Zane Harrison\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Justina Kane\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Ethan Curry\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"4Kato Lindsay\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true}]}
"))
(defpage "/admin/data/getusers/4" []
  (str "
{\"users\":[{\"id\":1,\"name\":\"5Naida Howard\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Theodore Atkins\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Blythe Baldwin\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Todd Gomez\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Abigail Hansen\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":false},
{\"id\":1,\"name\":\"5Danielle Mejia\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Zane Harrison\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Justina Kane\",\"level\":\"paid 1\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Ethan Curry\",\"level\":\"unpaid\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true},
{\"id\":1,\"name\":\"5Kato Lindsay\",\"level\":\"paid 2\",\"usage\":\"blah blah\",\"admin\":false,\"active\":true}]}
"))

(defpage "/admin/data/getplugins/0" []
  (str "
{\"plugins\":[{\"id\":1,\"name\":\"goiuhg\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"wergrth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"asdvyjk\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"fghjetyu\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"rtyjrtj\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":false},
{\"id\":1,\"name\":\"rtyjurtjy\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"wertherth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"tyuoityuk\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"qwreferh\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"eryhrtyh\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true}]}
"))

(defpage "/admin/data/getplugins/1" []
  (str "
{\"plugins\":[{\"id\":1,\"name\":\"goiuhg\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2wergrth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2asdvyjk\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2fghjetyu\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2rtyjrtj\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":false},
{\"id\":1,\"name\":\"2rtyjurtjy\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2wertherth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2tyuoityuk\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2qwreferh\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"2eryhrtyh\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true}]}
"))

(defpage "/admin/data/getplugins/2" []
  (str "
{\"plugins\":[{\"id\":1,\"name\":\"3goiuhg\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3wergrth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3asdvyjk\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3fghjetyu\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3rtyjrtj\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":false},
{\"id\":1,\"name\":\"3rtyjurtjy\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3wertherth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3tyuoityuk\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3qwreferh\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"3eryhrtyh\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true}]}
"))

(defpage "/admin/data/getplugins/3" []
  (str "
{\"plugins\":[{\"id\":1,\"name\":\"4goiuhg\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4wergrth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4asdvyjk\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4fghjetyu\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4rtyjrtj\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":false},
{\"id\":1,\"name\":\"4rtyjurtjy\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4wertherth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4tyuoityuk\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4qwreferh\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"4ryhrtyh\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true}]}
"))

(defpage "/admin/data/getplugins/4" []
  (str "
{\"plugins\":[{\"id\":1,\"name\":\"5goiuhg\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5wergrth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5asdvyjk\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5fghjetyu\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5rtyjrtj\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":false},
{\"id\":1,\"name\":\"5rtyjurtjy\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5wertherth\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5tyuoityuk\",\"level\":\"paid 1\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5qwreferh\",\"level\":\"unpaid\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true},
{\"id\":1,\"name\":\"5eryhrtyh\",\"level\":\"paid 2\",\"category\":\"blah blah\",\"admin\":\"description of the plugin here to edit\",\"active\":true}]}
"))
(defpage "/admin/data/getrowcountusers" []
  (str "50"))
(defpage "/admin/data/getrowcountplugins" []
  (str "50"))

(defpage "/admin/get-worlds" []
  "/admin/get-worlds")

