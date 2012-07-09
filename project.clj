(defproject cavapanel "0.1.0-tmarble1"
            :description "Cava Panel web site"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.2-SNAPSHOT"]
                           [org.clojars.tavisrudd/redis-clojure "1.3.1"]
                           [clj-time "0.3.3"]
                           [future-contrib "1.3.0-tmarble7"]
                           [password-storage "1.0.0-tmarble1"]
                           [clj-json "0.4.3"]
                           [org.apache.xmlrpc/xmlrpc-client "3.1.3"]
                           [javax.mail/mail "1.4.2"]]
            :aot [cavapanel.views.common
                  cavapanel.views.welcome
                  cavapanel.views.register
                  cavapanel.views.panel
                  cavapanel.views.admin
                  cavapanel.views.support
                  ;; cavapanel.views.tests
                  cavapanel.views.admindata
                  cavapanel.views.panelserver]
            :main cavapanel.server ) ;; note please keep blank before paren
