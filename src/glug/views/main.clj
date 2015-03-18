(ns glug.views.main
  (require [hiccup.page :as page]
           [hiccup.form :as form])
  (:gen-class))

(defn index []
  (page/html5
    [:head
     [:title "Hello World"]]
    [:body
     [:div {:id "content"} "Hello World"]]))

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
