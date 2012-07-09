(ns cavapanel.email
  (:import [javax.mail
            Authenticator
            Message$RecipientType
            PasswordAuthentication
            Session Transport]
           [javax.mail.internet
            InternetAddress
            MimeMessage]))

(defn mail [& m]
  "m: options, which may include:
  :host (such as 'localhost')
  :port (such as 25)
  :user (such as 'joe@blow.net')
  :password (such as 'p455w0rd')
  :ssl (such as true)
  :subject (such as 'hello')
  :text (such as 'nice to see you')"
  (let [mail (apply hash-map m)
        props (java.util.Properties.)
        username (try
                   (:user mail)
                   (catch Exception e
                     false))
        password (try
                   (:password mail)
                   (catch Exception e
                     false))]

    (doto props
      (.put "mail.smtp.host" (:host mail))
      (.put "mail.smtp.port" (:port mail))
      (.put "mail.smtp.socketFactory.port"  (:port mail)))
    
    (if-not (empty? username)
      (if-not (empty? password)
        (.put props "mail.smtp.user" username)
        (.put props "mail.smtp.auth" "true")))

    (if (= (:ssl mail) true)
      (doto props
        (.put "mail.smtp.starttls.enable" "true")
        (.put "mail.smtp.socketFactory.class"
              "javax.net.ssl.SSLSocketFactory")
        (.put "mail.smtp.socketFactory.fallback" "false")))

    (let [authenticator (proxy [Authenticator] []
                          (getPasswordAuthentication
                            []
                            (PasswordAuthentication. username
                                                     password)))
          recipients (reduce #(str % "," %2) (:to mail))
          session (if-not (empty? username)
                    ; True
                    (Session/getInstance props authenticator)
                    ; False
                    (Session/getInstance props))
          msg     (MimeMessage. session)]

      (.setFrom msg (InternetAddress. "no-reply@junjible.com"))

      (.setRecipients msg
                      (Message$RecipientType/TO)
                      (InternetAddress/parse recipients))

      (.setSubject msg (:subject mail))
      (.setText msg (:text mail))
      (Transport/send msg))))
