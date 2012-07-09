(ns cavapanel.views.forgotpass
  (:require [cavapanel.views.common :as common]
            [hiccup.page-helpers :as web]
            [noir.validation :as valid]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [noir.response :as response]
            [cavapanel.database :as db])
  (:use [clojure.java.shell :only [sh]]
        [noir.core]
        [hiccup.core]
        [redis.core :only [with-server]]
        [cavapanel.email]))

(defpartial error-item [[first-error]]
  [:span.error first-error])

(defn reset-password [email user]
  (let
      [account (str "user:" user)
       db-email (db/hget account "email")
       db-password (db/hget account "pass")
       reset (= db-email email)]
    (if (= reset true)
      ; True
      (do
        (try
          (mail :port 25
                :ssl false
                :to [email]
                :subject (str "Junjible Support - Reset Password for " user)
                :text (str "Your Jubjible password is " db-password)
                :host "localhost")
          (println "Sending e-mail to" email)
          (catch java.rmi.activation.ActivationException e
            (println e)
            false))
        true)
      ; False
      false) ))

(defpage "/account/reset" {:as userreg}
  (with-server (db/redis-options)
    (common/layout
     "intro"
     (str
      ;;(common/layout-header)
      (html ;; about
       [:hgroup
        [:div.logo-abs]
        [:section
         [:div.register.graybox
          [:form {:method "POST", :action "/account/reset"}
           [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
            [:tr
             [:td]
             [:td "Reset your password"]
             ]
            [:tr
             [:td "Username:"]
             [:td
              [:input {:type "text" :name "user" :maxlength 50}]]
             ]
            [:tr
             [:td "Account E-mail:"]
             [:td [:input {:type "text", :name "email", :maxlength "50",
                           :value (:email userreg)}]]
             [:td {:style "text-align:left;"}]
             ]
            [:tr
             ;;           [:td {:colspan "2", :align "left"}
             [:td {:colspan "2", :align "left", :style "text-align;left"}
              [:input {:type "hidden", :name "subjoin", :value "1"}]
              [:input#login-button.login {:type "submit", :value "Reset Account"}]
              ]
             ]
            ]
           ]
          [:div.clear]
          ]
         ]
        ]
       )
      (common/layout-footer)
      ))))


(defpage [:post "/account/reset"] {:as userreg}
  (with-server (db/redis-options)
    (if (reset-password (:email userreg)
                        (:user userreg))
      (common/layout
       "intro"
       (str
        (common/layout-header)
        (html ;; SUCCESS
         [:hgroup
          [:div.logo-abs]
          [:section
           [:div.register.graybox
            [:h3 "Your password has been reset"]
            "Your new password has been sent to " [:b (:email userreg)]
            [:form#login-form {:method "POST", :action "/login"}
             [:span
              [:input (let [attr {:type "checkbox", :name "remember"}
                            checked (if (db/lastuser) (hash-map "checked" "true"))]
                        (merge attr checked))]
              [:font {:size "1"} "Remember me &nbsp;"]
              [:input {:type "text", :name "user", :class "text-field", :maxlength "30", :size "40", :value (db/lastuser)}]

              [:input {:type "password", :name "pass", :class "text-field", :maxlength "30", :size "40", :value ""}]

              
              [:input {:type "hidden", :name "sublogin", :value "1"}]
              [:input#login-button.login {:type "submit", :value "Login"}]
              ]]
            [:div.clear]
            ]
           ]
          ]
         )
        ))
      (render "/account/reset" userreg))))

