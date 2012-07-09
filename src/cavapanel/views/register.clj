(ns cavapanel.views.register
  (:require [clojure.string :as str]
            [cavapanel.views.common :as common]
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
        [cavapanel.utils]))

(defpartial error-item [[first-error]]
  [:span.error first-error])

;; (defpage "/signup" [{:keys [user pass email]}]
; (defpage "/signup" []
(defpage "/signup" {:as userreg}
  (common/layout
   "intro"
   (str
    ;;(common/layout-header)
    (html ;; about
     [:hgroup
[:div.logo-abs]
      [:section
       [:div.register.graybox
        [:form {:method "POST", :action "/signup"}
         [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
          [:tr
           [:td {:colspan "2", :align "left", :style "text-align;left"}
            (valid/on-error :user error-item)
            ]
           ]
          [:tr
           [:td "Username:"]
           [:td [:input {:type "text", :name "user", :maxlength "30",
                         :value (:user userreg)}]]
           [:td {:style "text-align:left;"}]
           ]
          [:tr
           [:td {:colspan "2", :align "left", :style "text-align;left"}
            (valid/on-error :pass error-item)
            ]
           ]
          [:tr
           [:td "Password:"]
           [:td [:input {:type "password", :name "pass", :maxlength "30",
                         :value (:pass userreg)}]]
           [:td {:style "text-align:left;"}]
           ]
          [:tr
           [:td {:colspan "2", :align "left", :style "text-align;left"}
            (valid/on-error :pass2 error-item)
            ]
           ]
          [:tr
           [:td "Re-enter password:"]
           [:td [:input {:type "password", :name "pass2", :maxlength "30",
                         :value ""}]]
           [:td {:style "text-align:left;"}]
           ]
          [:tr
           [:td {:colspan "2", :align "left", :style "text-align;left"}
            (valid/on-error :email error-item)
            ]
           ]
          [:tr
           [:td "Email:"]
           [:td [:input {:type "text", :name "email", :maxlength "50",
                         :value (:email userreg)}]]
           [:td {:style "text-align:left;"}]
           ]
          [:tr
;;           [:td {:colspan "2", :align "left"}
           [:td {:colspan "2", :align "left", :style "text-align;left"}
            [:input {:type "hidden", :name "subjoin", :value "1"}]
            [:input#login-button.login {:type "submit", :value "register"}]
            ]
           ]
          ]
         ]
        "Register now and get all kinds of Minecraft love from your friends at Junjible"
        [:div.clear]
        ]
       ]
      ]
     )
    (common/layout-footer)
    )))

(defn reg-valid? [{:keys [user pass pass2 email]}]
  (verbose-msg "reg-valid user=" user
               "pass=" pass
               "pass2=" pass2
               "email=" email)
  (valid/rule (valid/min-length? user 2)
             [:user "The username must have more than 2 letters."])
  (valid/rule (valid/min-length? pass 6)
             [:pass "The password must have more than 6 letters."])
  (valid/rule (= pass pass2)
             [:pass2 "The passwords must match"])
  (valid/rule (valid/min-length? email 4)
              [:email "The e-mail must have more than 4 letters."])
  (let [valid (not (valid/errors? :user :pass :pass2 :email))]
    (verbose-msg "reg-valid? = " valid)
    valid
    ))

  ;; make sure user and email are not used
;; upon trouble
;;        (valid/set-error :email "Sorry, your e-mail is not authorized.")
;; return false
(defn signup [{:keys [user pass email]}]
  (if (db/valid-user? user)
    (do (valid/set-error :user "Sorry, that username is already taken.")
        false)
    (if (db/emailused? email)
      (do (valid/set-error :user "Sorry, that e-mail is claimed by another user.")
          false)
      (let [msg (db/update-user user {:pass pass, :email email} true)]
        (verbose-msg msg)
        true))))

(defn allowed-email? [email & attempt]
  (let [allowed (db/get-allowed-emails)
        email1 (str/lower-case email)]
    ;; (verbose-msg "allowed-email? allowed=" allowed "\n email1=" email1)
    (if (contains? allowed email1)
      (do
        ;; (println "new email: " email " is allowed:" allowed)
        true)
      (do
        ;; (println "new email: " email " not in:" allowed)
        (verbose-msg "unauthorized registration: " email) 
        (valid/set-error :email "Sorry, your e-mail is not authorized.")
        false))))

(defpage [:post "/signup"] {:as userreg}
  (with-server (db/redis-options)
    (verbose-msg "/signup userreg=" userreg)
    (if (and (reg-valid? userreg)
             (allowed-email? (:email userreg))
             (signup userreg))
      (common/layout
       "intro"
       (str
        (common/layout-header)
        (html ;; SUCCESS
         [:hgroup
          [:div.logo-abs]
          [:section
           [:div.register.graybox
            [:h3 "Registered!"]
            "Thank you " [:b (:user userreg)]
            " ... You may now login:"
            [:form#login-form {:method "POST", :action "/login"}
             [:span
              [:input (let [attr {:type "checkbox", :name "remember"}
                            checked (if (db/lastuser) (hash-map "checked" "true"))]
                        (merge attr checked))]
              [:font {:size "1"} "Remember me &nbsp;"]
              [:input {:type "text", :name "user", :class "text-field", :maxlength "30", :size "40", :value (db/lastuser)}]

              [:input {:type "password", :name "pass", :class "text-field", :maxlength "30", :size "40", :value ""}]

              ;; [:input {:type "hidden", :name "sublogin", :value "1"}]
              [:input#login-button.login {:type "submit", :value "Login"}]
              ]]
            [:div.clear]
            ]
           ]
          ]
         )
        ))
      (render "/signup" userreg))))

(defpage [:post "/login"] {:as userlogin}
  (with-server (db/redis-options)
    (let [user (:user userlogin)]
      (verbose-msg "/login" user)
      ;;    (if (db/login? userlogin)
      (let [loginresult (db/login? userlogin)]
        ;; (println "loginresult:" loginresult)
        (if loginresult
          (let [flash (session/flash-get)]
            ;; DEBUG
            ;; (println "user URI flash:" flash)
            (response/redirect (if (nil? flash) "/panel" flash)))
          (do
            (error-msg "Login failed for: " user)
            (common/layout
             "intro"
             (str
              (html ;; Login Failed
               [:hgroup
                [:div.logo-abs]
                [:section
                 [:div.register.graybox
                  [:h3 "Login Failed!"]
                  [:p
                   [:a {:href "/#login"} "Retry now"]
                   "&nbsp; or "
                   [:a {:href "/signup"} "sign up"]
                   "&nbsp; or "
                   [:a {:href "/account/reset"} "reset your password"]
                   ]
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
              ))))))))

(defpage [:any "/logout"] {}
  (with-server (db/redis-options)
    (do
      (if (db/user?) (db/logout))
      (response/redirect "/"))))


;; NOTE currently NOT wrapped with with-server !!!!!
(defpage "/debug" []
  (common/layout
   "intro"
   (str
    (common/layout-header)
    (html ;; about
     [:div.debug
      "This is the debug page"
      [:ul
       [:li "allowed:emails: "
        ;; (let [allowed (set (smembers "allowed:emails"))]
        (let [allowed nil]
          (flush)
          (println "hi, I'm in the debug page now:" allowed)
          ;; (println "waiting...")
          ;; (flush)
          ;; (Thread/sleep 10000)
          ;; (flush)
          ;; (println "...done")
          ;; (flush)
          ;; (str allowed))
          )
        ]
       [:li "environment: " ]
       [:textarea.debug {:rows "20", :cols "132"}
        (str (sh "cal" "-h"))
        ;; (let [env (java2hash (System/getenv))]
        ;;   (escape-html (apply str (for [k (sort (keys env))]
        ;;                             (str (name k) " = " (k env) "\n")))))
        ]
       [:li "cava-instance:" ]
       [:textarea.debug {:rows "20", :cols "132"}
        ;; (let [cmd (list "cal" "-h")]
        ;; (let [junjible-instance (str (System/getenv "JUNJIBLE_SERVER") "/bin/junjible-instance")
        ;;       cmd (list junjible-instance "-d" "-v" "status")]
        ;;   (println "executing:" cmd)
        ;;   (escape-html (str (apply sh cmd))))
        ]
       ]
      ])
    (common/layout-footer)
    )))
