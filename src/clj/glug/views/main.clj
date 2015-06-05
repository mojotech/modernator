(ns glug.views.main
  (require [hiccup.page :as page]
           [hiccup.form :as form])
  (:gen-class))

(defn index [_]
  (page/html5
    [:head
     [:meta {:charset "utf8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge, chrome=1"}]
     [:title "Booze"]
     (page/include-css "/public/css/styles.css")]

    [:link {:href "http://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700" :rel "stylesheet" :type "text/css"}]
    [:link {:href "http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" :rel "stylesheet" :type "text/css"}]
    [:body
     [:header.site-header
      [:h1.header-title "Modernator"]]
     [:section.forum
      [:div#app ""]]
     (page/include-js
       "http://underscorejs.org/underscore.js"
       "/public/js/app.js")]))

(defn signup []
  (page/html5
    [:head
     [:title "Signup"]]
    [:body
     (form/form-to
       [:post ""]
       [:div
        (form/label "email" "What's your email?")
        (form/email-field "email")]
       [:div
        (form/label "crowd" "What's the name of your crowd?")
        (form/text-field "crowd")]
       [:div
        (form/label "crowd_emails" "Who are the peeps in your crowd (list of emails, [we won't email them until we know you're legit])?")
        (form/text-area "crowd_emails")]
       (form/submit-button "Signup"))]))

(defn signup-confirm []
  (page/html5
    [:head
     [:title "Signup"]]
    [:body "You should be getting a link in your email"]))

(defn admin-welcome-email [auth-token]
  (page/html5
    [:body
     [:p "Hey There!"]
     [:p "We got a signup from your email on "
      [:a {:href (str (System/getenv "GLUG_URL"))} "Glug"]
      ", So if you legitimately signed up for this, how about you "
      [:a {:href (str (System/getenv "GLUG_URL") "confirm-crowd/" auth-token)}
       "send invites to your crowd and start using glug!"]]
     [:p "If you didn't sign up for this, sorry about that, looks like you got trolled."]
     [:p "Happy Drinking,"]
     [:p "MojoTech"]]))

(defn user-welcome-email [auth-token]
  (page/html5
    [:body
     [:p "Hey There!"]
     [:p "You were added to a group to help crowdsource your drinks."]
     [:p "So why don't you go ahead and "
      [:a {:href (str (System/getenv "GLUG_URL") "confirm-user/" auth-token)}
       "start choosing some drinks"]
      "."]
     [:p "Happy Drinking,"]
     [:p "MojoTech"]]))
