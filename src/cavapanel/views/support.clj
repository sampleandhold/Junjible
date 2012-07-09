(ns cavapanel.views.support
  (:import [org.apache.xmlrpc.client.XmlRpcClient]
           [org.apache.xmlrpc.client.XmlRpcClientConfigImpl])
  (:require [cavapanel.views.common :as common]
            [hiccup.page-helpers :as web]
            [noir.validation :as valid]
            [noir.session :as session]
            [noir.response :as response]
            [noir.cookies :as cookies]
            [clojure.pprint]
            [cavapanel.database :as db])
  (:use [noir.core]
        [hiccup.core]
        [cavapanel.email]
        [redis.core :only [with-server]]
        [clojure.java.io :only (as-url)]))

(def rpc-auth {:user "jenkins"
               :password "safe4blocs"})

(def trac-end-point
  "https://mail.chatsubo.net/maas-trac/login/rpc")

(def ticket 0)

(defn safe-input
  [input]
  (let
      [lt (clojure.string/replace input #"<" "&lt;")
       gt (clojure.string/replace lt #">" "&gt;")
       q  (clojure.string/replace gt #"\"" "&quot;")
       return q]
    return))


(defpage [:post "/support/submit"] {:as ticket-vars} []
  (let
      [config (doto (org.apache.xmlrpc.client.XmlRpcClientConfigImpl.)
                (.setServerURL (as-url trac-end-point))
                (.setBasicPassword (:password rpc-auth))
                (.setBasicUserName (:user rpc-auth)))
       name (safe-input (:name ticket-vars))
       email (safe-input (:email ticket-vars))
       comments (safe-input (:comments ticket-vars))]
    (if (or (empty? name) (empty? email) (empty? comments))
      (println "Invalid or incomplete support form")
      (try
        (let
            [debug true
             attributes {"milestone" "customer support"
                         "version" "0.9"
                         "component" "user experience"}
             client (doto (org.apache.xmlrpc.client.XmlRpcClient.)
                      (.setConfig config))
             ticket-number (.execute client
                                     "ticket.create"
                                     [; Summary
                                      (str "Support Ticket: " name ": " email)
                                      ; Description
                                      comments
                                      ; Attributes
                                      attributes])
             message (str "Your Junjible Support Ticket Number: " ticket-number
                          "\r\nIssue Comments: " comments)]

          (println "New Support Ticket: "
                   ticket-number 
                   (str " "
                        (if debug
                          (str " attributes: " attributes))))
          
          (if debug
            (clojure.pprint/pprint (.toString client)))
          
          ; Mail ticket Confirmation
          (try
            (mail :port 25
                  :ssl false
                  :to [(clojure.string/replace email #" " "")] ; Removes all spaces (fixes spaces at header)
                  :subject (str "Junjible Support - Confirmation " ticket-number)
                  :text message
                  :host "localhost")
            (println "Sending e-mail to" email)
            (catch java.rmi.activation.ActivationException e
              (println e)
              (str "Error")))

          ; Return Ticket #
          (str "Your Ticket Number: " ticket-number))
        (catch java.rmi.activation.ActivationException e
          (spit "logs/support-errors.log" e :append true)
          (str "Sorry, but an error occured! Please try again at another time.")))) ))
    
(defpage [:get "/support"] []
  (with-server (db/redis-options)
    (let [user (db/user?)
          umap (if user (db/get-user user))
          email (if user (:email umap))]
      (common/layout
       "support"
       (str
        (common/layout-header)
        (html
         [:script {:src "/js/support.js"}]
         [:hgroup
          ;; [:div.logo-abs]
          [:section
           [:div.register.graybox
            [:form {:method "POST", :action "/support"}
             [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
              [:tr
               [:td]
               [:td {:id "ticket-number"
                     :name "ticket-number"
                     :class "ticket-number"
                     :style "text-align:center;"}]]
              [:tr
               [:td "Name:"]
               (if user
                 [:td {:style "text-align:left;"} [:input {:type "text", :id "name", :name "name", :maxlength "50" :value user}]]
                 [:td {:style "text-align:left;"} [:input {:type "text", :id "name", :name "name", :maxlength "50"}]])
               [:td {:style "text-align:left;"}]
               ]
              [:tr
               [:td "Email:"]
               (if user
                 [:td {:style "text-align:left;"} [:input {:type "text", :id "email" :name "email", :maxlength "50" :value email}]]
                 [:td {:style "text-align:left;"} [:input {:type "text", :id "email" :name "email", :maxlength "50"}]])
               [:td {:style "text-align:left;"}]
               ]
              [:tr
               [:td "Comments:"]
               [:td [:textarea {:id "comments", :name "comments", :rows "10", :cols "80"}]]
               [:td {:style "text-align:left;"}]
               ]
              [:tr
               [:td {:colspan "2", :align "left", :style "text-align;left"}
                [:input#login-button.login {:type "button" :value "submit" :onclick "javascript:createTicket();"}]
                ]
               ]
              ]
             ]
            "Enter a support request"
            [:div.clear]
            ]
           ]
          ]
         )
        (common/layout-footer))))))

(comment

  This is probably not needed considering the ajax response in the :get method
  
  (defpage [:post "/support"] {:as support-req}
    (let [user (db/user?)
          umap (if user (db/get-user user))
          name (if user user (:name support-req))
          email (if user (:email umap) (:email support-req))
          comments (:comments support-req)]
      ;; Ving: this around the place where you will
      ;; call another function to the make the XMLRPC call
      ;; and yet another function to send the confirmation e-mail
      (common/layout
       "support"
       (str
        (common/layout-header)
        (html
         [:hgroup
          ;; [:div.logo-abs]
          [:section
           [:div.register.graybox
            [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
             [:tr
              [:td "Logged in:"]
              [:td {:style "text-align:left;"} (if user "yes" "no")]
              [:td {:style "text-align:left;"}]
              ]
             [:tr
              [:td "Name:"]
              [:td {:style "text-align:left;"} [:input {:type "text", :id "name", :name "name", :maxlength "50" :value user}]]
              [:td {:style "text-align:left;"}]
              ]
             [:tr
              [:td "Email:"]
              [:td {:style "text-align:left;"} [:input {:type "text", :id "email" :name "email", :maxlength "50" :value email}]]
              [:td {:style "text-align:left;"}]
              ]
             [:tr
              [:td "Comments:"]
              [:td {:style "text-align:left;"} comments]
              [:td {:style "text-align:left;"}]
              ]
             ]
            [:div.clear]
            ]
           ]
          ]
         )
        (common/layout-footer)))))
) ; End of Comment


