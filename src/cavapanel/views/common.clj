(ns cavapanel.views.common
  (:require [noir.response :as response]
            [noir.validation :as valid]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [hiccup.page-helpers :as web]
            [cavapanel.database :as db])
  (:use [noir.core :only [defpartial]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers]
        [redis.core :only [with-server]]
        [future-contrib.pphtml]))

(defpartial layout [bodyclass content]
  (pphtml-str 
   (html5
    [:head
     [:title "Junjible"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "description", :content "Ipsum Lorem Minecraftus Servus"}]
     [:meta {:name "keywords", :content "Keywordi Minecraftibus SEO Maximus"}]
     [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0"}]
     ;; (include-css "/css/reset.css") ;; media="screen"
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/reset-first.css"}]
     ;; <!-- 1000px Grid styles for IE -->
     ;; <!--[if lte IE 9]><link rel="stylesheet" href="/css/ie.css" type="text/css" media="screen" /><![endif]-->
	
     ;; <!-- Help IE understand the HTML5 new elements -->
     ;; <!--[if IE]><script type="text/javascript" src="http://html5shiv.googlecode.com/svn/trunk/html5.js"></script><![endif]-->
     ;; (include-css "/css/1000.css") ;; media="screen"
     ;; (include-css "/css/styles.css") ;; media="screen"
     ;; (include-css "/css/tooltip.css") ;; media="screen"
     ;; (include-css "/css/jquery.rating.css") ;; media="screen"
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/1000.css"}]
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/styles.css"}]
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/tooltip.css"}]
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/jquery.rating.css"}]
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/css/cavapanel.css"}]
     [:link {:rel "stylesheet", :type "text/css", :media "screen"
             :href "/slider/anythingslider.css"}]
     (include-css "/css/webfontkit/stylesheet.css") ;; charset="utf-8"
     ;; <!-- Enable gradient support for IE9 -->
     ;; <!--[if gte IE 9]>
     ;; 	<style type="text/css">
     ;; 	.gradient {
     ;; 		filter: none;
     ;; 		 }
     ;; 	</style>
     ;; <![endif]-->
     ;; (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
     (include-js "/js/jquery.min.js") ;; MINIFIED
     ;; (include-js "/js/jquery.js") ;; UNCOMPRESSED
     (include-js "/js/scrollbar.js")
     (include-js "/js/jquery.rating.js")
     (include-js "/js/easing.js")
     (include-js "/js/tooltip.js")
     (include-js "/js/jquery.urlencode.js")
     (include-js "/slider/jquery.anythingslider.js")
     (include-js "/js/css3-mediaqueries.js")
     (include-js "/js/junjible.js")
     (include-js "/js/instances.js")
     (include-js "/js/admin.js")
     (include-js "/js/tabswitch.js")

     ;; (include-js "/js/groups.js")
     ;; (javascript-tag (str "window.username = \"Guest\";\n"
     ;;                      "$(function(){\n"
     ;;                      "    $(\"#tabNav.pan li a\").tipTip({maxWidth: 300, edgeOffset: 2, fadeIn: 300, fadeOut: 300, delay:0, activation: \"hover\", keepAlive: false, defaultPosition: \"bottom\"});\n"
     ;;                      "});\n"
     ;;                      ))
     [:script {:type "text/javascript"}
      (str 
       "window.username = \"" (if-let [user (db/user?)] user "Guest") "\";\n"
       "$(function(){\n"
       "    $(\"#tabNav.pan li a\").tipTip({maxWidth: 300, edgeOffset: 2, fadeIn: 300, fadeOut: 300, delay:0, activation: \"hover\", keepAlive: false, defaultPosition: \"bottom\"});\n"
       "});\n"
       )]
     ;; (include-js "/js/jquery.aycs.chat.js")
     ;;      <script type="text/javascript">
     ;; window.username = "<? echo $session->username; ?>";
     ;;     			$(function(){
     ;;     		$("#tabNav li a").tipTip({maxWidth: 300, edgeOffset: 2, fadeIn: 300, fadeOut: 300, delay:0, activation: "hover", keepAlive: false, defaultPosition: "bottom"});
     ;;     	});
     ;;     </script>
     ;; <!-- Google Analytics -->
     ;;     <script type="text/javascript">
     ;;       /*
     ;;       var _gaq = _gaq || [];
     ;;       _gaq.push(['_setAccount', '']);
     ;;       _gaq.push(['_trackPageview']);
     ;;       (function() {
     ;;         var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
     ;;         ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
     ;;         var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
     ;;       })();
     ;;      */
     ;;     </script>
     ;;  <link href="/uploadify/uploadify.css" type="text/css" rel="stylesheet" />
     ;; <script type="text/javascript" src="/uploadify/swfobject.js"></script>
     ;; <script type="text/javascript" src="/uploadify/jquery.uploadify.v2.1.4.min.js"></script>
     ]
    [(keyword (str "body." bodyclass))
     [:div#top]
     content])))

(defpartial layout-refresh [url content]
  (pphtml-str 
   (html5
    [:head
     [:meta { :http-equiv "refresh", :content (str "5; url=" url) } ]
     [:title "my-website"]
     (include-js "/js/mydate.js")
     (include-css "/css/tom.css")]
    [:body
     [:div#wrapper
      content]])))

(defpartial layout-login [& content]
  (html5
   [:h3 "Login" ]
   [:div#loginform
    [:form#login-form {:method "POST", :action "/login"}
     [:table {:align "left", :border "0", :cellspacing "0", :cellpadding "3"}
      [:tr
       [:td "Username:"]
       [:td [:input {:type "text", :name "user", :maxlength "30", :value (db/lastuser)}]]
       [:td.left-align]
       ]
      [:tr
       [:td "Password:"]
       [:td [:input {:type "password", :name "pass", :maxlength "30", :value ""}]]
       [:td.left-align]
       ]
      [:tr
       [:td {:colspan "2", :align "left"}
        [:input (let [attr {:type "checkbox", :name "remember"}
                      checked (if (db/lastuser) (hash-map "checked" "true"))]
                  (merge attr checked))]
        [:font {:size "2"} "Remember me &nbsp;&nbsp;&nbsp;&nbsp;"]
        [:input {:type "hidden", :name "sublogin", :value "1"}]
        [:input#login-button {:type "submit", :value "Login"}]
        ]
       ]
      ]
     ]
    
    ]
   ))

(defpartial layout-header [& content]
  (html5
   [:header.gradient
    [:nav
     [:div.twocol [:a#logo {:href "/"} (web/image "/images/lil-logo.png")]]
     (if (db/user?)
       (vector ;; user authenticated session
        :div#nav-list
        [:a.nav-link {:href "/support", :rel "support"} [:span "support"]]
        [:a.nav-link {:href "/about", :rel "about"} [:span "about"]]
        [:a.nav-link {:href "/panel", :rel "panel"} [:span "panel"]]
        [:a.nav-link {:href "/logout", :rel "logout"} [:span "log out"]]
        [:a.nav-link {:href "/admin", :rel "admin", :style "display:none;", :id "admin-link"} [:span "admin"]]
        )
       (vector ;; NOT logged in
        :div#nav-list
        [:a.nav-link {:href "/support", :rel "support"} [:span "support"]]
        [:a.nav-link {:href "/signup", :rel "signup"} [:span "sign up"]]
        [:a.nav-link {:href "/about", :rel "about"} [:span "about"]]
        [:a.nav-link {:href "/", :rel "login"} [:span "log in"]]
        ))
     ]
    ]
   ))

(defpartial layout-footer [& content]
  (html5
   [:footer
    [:section
     [:div.sixcol "&#169;Copyright 2012 Chresco, LLC. All Rights Reserved."]
     [:div {:class "sixcol last"} 
      [:ul 
       ;; [:li#twitter
       ;;  ;; FIX: comment out BOGUS LINKS
       ;;  " "
       ;;  ;; [:a {:href "http://twitter.com/npanel",
       ;;  ;;      :target "_blank",
       ;;  ;;      :title "Follow NPanel on Twitter" }
       ;;  ;;  "Twitter"]
       ;;  ]
        [:li#facebook
         [:a {:href "https://www.facebook.com/pages/Junjible/300726330012986",
              :target "_blank",
              :title "Follow Junjible on Facebook" }
          (web/image "/images/icon_facebook_gray.png")]
         ]
       ]
      ]
     ]
    ]
   [:div#obscura]
   [:div#flyover
    [:div#flyover-content
     (layout-login)
     ]
    [:div.close-box (web/image "/images/close.png")]
    ]
   (include-js "/js/footer.js")
   ))

(defpartial root-page [& content]
  (html5
   [:head
    [:title "Junjible"]
    [:meta
     {:http-equiv "content-type"
      :content "text/html;charset=UTF-8"}]
    [:meta
     {:name "description"
      :content ""}]
    [:meta
     {:name "keywords"
      :content ""}]
    (include-css "/css/intro.css")
    (include-js "/js/junjible.js")
    (include-js "/js/jquery.min.js")
    (include-js "/js/jquery.scrollTo.js")
    (include-js "/js/jquery.nivo.slider.js")
    (include-js "/js/root-page.js")]
   [:body
    [:div
     {:class "a_top"}
     "&nbsp;"]
    [:div
     {:class "bar"}
     [:div
      {:class "logo"}
      [:a
       {:href "javascript:void(0);"
        :class "b_logo"}
       [:img
        {:src "redesign/images/logo.png"
         :alt "Junjible Logo"}
        ]]]; div, a, img
     [:div
      {:class "nav"}
      [:div
       {:class "login-holder"}
       [:form#login-form
        {:method "POST"
         :action "/login"}]
       [:span
        [:input
         (let
             [attr {:type "checkbox"
                    :name "remember"}
              checked (if (db/lastuser)
                        {:checked "true"})]
           (merge attr checked))]
        [:font
         {:size "1"}
         "Remember me &nbsp;"]
        [:input
         {:type "text"
          :name "user"
          :class "text-field"
          :maxlength 30
          :size 40
          :value (db/lastuser)}]
        [:input
         {:type "password"
          :name "pass"
          :class "text-field"
          :maxlength 30
          :size 40
          :value ""}]
        [:input
         {:type "hidden"
          :name "sublogin"
          :value 1}]
        [:input#login-button.login
         {:type "submit"
          :value "Login"}]
        [:span
         {:class "or"}
         [:img
          {:src "/images/or.png"}]]
        [:input
         {:type "button"
          :class "login b_contact"
          :value "sign up"}]
        [:input
         {:type "button"
          :class "login b_support"
          :value "support"}]
        [:input
         {:type "button"
          :class "login b_about"
          :value "about"}]
        ]]]]; span, div, div, div
    [:div
     {:class "wrapper"}
     [:div
      {:class "intro"}
      [:div
       {:class "intro_cta"}
       [:a
        {:href "javascript:void(0)"
         :class "b_cta"}]]]; div, div, a
     [:div
      {:class "work"}
      [:div
       {:class "a_port"}]
      [:div
       {:id "slider"
        :class "nivoSlider"}
       [:a
        {:href ""
         :target "_blank"}
         [:img
          {:src "/images/intro/img1.jpg"
           :title "#htmlcaption1"
           :alt ""}]]
       [:a
        {:href ""
         :target "_blank"}
        [:img
         {:src "/images/intro/img2.jpg"
          :title "#htmlcaption2"
          :alt ""}]]
       [:a
        {:href ""
         :target "_blank"}
        [:img
         {:src "/images/intro/img3.jpg"
          :title "#htmlcaption3"
          :alt ""}]]
       [:a
        {:href ""
         :target "_blank"}
        [:img
         {:src "/images/intro/img4.jpg"
          :title "#htmlcaption4"
          :alt ""}]]
      [:input
       {:type "button"
        :class "login b_contact"
        :value "sign up"}]]
      [:div
       {:id "htmlcaption1"
        :class "nivo-html-caption"}
       [:h1 "Lorem Ipsum"]
       [:ul
        {:class "things"}
        [:li "asdasdasdasdasd"]
        [:li "erewrwerwerwewer"]
        [:li "asdasdcvxcvxcwr"]
        [:li "cpvinpxcvcvnpxcv"]]]
      [:div
       {:id "htmlcaption2"
        :class "nivo-html-caption"}
        [:h1 "Lorem Ipsum 2"]
        [:ul
         {:class "things"}
         [:li "asdasdasdasdasd"]
         [:li "erewrwerwerwewer"]
         [:li "asdasdcvxcvxcwr"]
         [:li "cpvinpxcvcvnpxcv"]]]
      [:div
       {:id "htmlcaption3"
        :class "nivo-html-caption"}
        [:h1 "Lorem Ipsum 3"]
        [:ul
         {:class "things"}
         [:li "asdasdasdasdasd"]
         [:li "erewrwerwerwewer"]
         [:li "asdasdcvxcvxcwr"]
         [:li "cpvinpxcvcvnpxcv"]]]
      [:div
       {:id "htmlcaption4"
        :class "nivo-html-caption"}
        [:h1 "Lorem Ipsum 4"]
        [:ul
         {:class "things"}
         [:li "asdasdasdasdasd"]
         [:li "erewrwerwerwewer"]
         [:li "asdasdcvxcvxcwr"]
         [:li "cpvinpxcvcvnpxcv"]]]
      [:div
       {:class "contact"}
       [:div
        {:class "a_contact"}]
       [:div
        {:class "contact-title"}]
       [:br
        {:style "clear: both;"}]
       [:div
        {:class "contact-form"}
        [:form
         {:method "POST"
          :action "/signup"}
         [:table
          {:align "left"
           :border 0
           :cellspacing 0
           :cellpadding 3}
          [:tr
           [:td
            {:colspan 2
             :align "left"
             :style "text-align:left"}]]
          [:tr
           [:td "Username &nbsp;"]
           [:td
            [:input
             {:type "text"
              :name "user"
              :class "text-field"
              :maxlength 30
              :value ""}]]
           [:td
            {:style "text-align:left;"}]]
          [:tr
           [:td
            {:colspan 2
             :align "left"
             :style "text-align:left;"}]]
          [:tr
           [:td "Password &nbsp;"]
           [:td
            [:input
             {:type "password"
              :name "pass"
              :class "text-field"
              :maxlength 30
              :value ""}]]
           [:td
            {:style "text-align:left;"}]]
          [:tr
           [:td
            {:colspan 2
             :align "left"
             :style "text-align:left;"}]]
          [:tr
           [:td "Email &nbsp;"]
           [:td
            [:input
             {:type "text"
              :name "email"
              :class "text-field"
              :maxlength 50
              :value ""}]]
           [:td
            {:style "text-align:left;"}]]
          [:tr
           [:td
            {:colspan 2
             :align "left"
             :style "text-align:right;"}
            [:input
             {:type "hidden"
              :name "subjoin"
              :value 1}]
            [:input#login-button.login
             {:type "submit"
              :value "register"}]]]
          ]]]]; div, div, form, table
      [:div
       {:class "contact-services"}
       [:h1 "WHAT WE DO"]
       "Minecraft Servers on Demand"
       [:br]
       "Plugins"
       [:br]
       "Killer Mods"
       [:br]
       "Tournaments"
       [:br]
       "Collaboration"
       [:br]
       "Robust Control Panel"
       [:br]
       "Free to Sign Up"]
      [:div
       {:class "contact-follow"}
       [:h1 "FOLLOW US"]
       [:div
        {:class "follow-links"}]]
      ]]; div, div
     ]; body
    ))
    
