(ns modernator.views.main
  (require [hiccup.page :as page]
           [hiccup.form :as form]
           [modernator.config :refer [config]])
  (:gen-class))

(defn index [_]
  (page/html5
    [:head
     [:meta {:charset "utf8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge, chrome=1"}]
     [:title "Modernator"]
     (page/include-css "/public/css/styles.css")]

    [:link {:href "http://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700" :rel "stylesheet" :type "text/css"}]
    [:link {:href "http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" :rel "stylesheet" :type "text/css"}]
    [:body
     [:header.site-header
      [:h1.header-title
       [:i.icon-logo.logo]
       "Modernator"]]
     [:div#app ""]
     (page/include-js
       "http://underscorejs.org/underscore.js"
       "/public/js/app.js")]))

(defn signup []
  (page/html5
    [:head
     [:title "Modernator :: Signup"]
     (page/include-css "/public/css/styles.css")
     [:link {:href "http://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700" :rel "stylesheet" :type "text/css"}]
     [:link {:href "http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" :rel "stylesheet" :type "text/css"}]]
    [:body
     [:header.site-header
      [:h1.header-title
       [:i.icon-logo.logo]
       "Modernator"]]
     [:section.content.sign-up
      (form/form-to
        [:post ""]
        [:div
         (form/label "email" "What's your email?")
         [:input#email {:name "email" :type "email" :class "form-field"}]]
        [:div
         (form/label "list" "What do you want the url to be?")
         [:input#list {:name "list" :type "text" :class "form-field"}]]
        [:div
         (form/label "purpose" "What's a one line description of the purpose of your modernator list?")
         [:input#purpose {:name "purpose" :type "text" :class "form-field"}]]
        [:div
         (form/label "list_emails" "Who are the people you want to contribute (list of emails)?")
         [:textarea#list_emails {:name "list_emails" :class "form-field"}]]
        [:input {:type "submit" :value "Sign Up" :class "btn sign-up btn-primary right"}])]]))

(defn signup-confirm []
  (page/html5
    [:head
     [:title "Modernator :: Signup"]
     (page/include-css "/public/css/styles.css")
     [:link {:href "http://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700" :rel "stylesheet" :type "text/css"}]
     [:link {:href "http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" :rel "stylesheet" :type "text/css"}]]
    [:body
     [:header.site-header
      [:h1.header-title
       [:i.icon-logo.logo]
       "Modernator"]]
     [:section.content.text-center
      "You should be getting a link in your email soon!"]]))

(defn admin-welcome-email [auth-token]
  (page/html5
    [:body
     [:p "Hey There!"]
     [:p "We got a signup from your email on "
      [:a {:href (str (config :modernator-url))} "Modernator"]
      ", So if you legitimately signed up for this, how about you "
      [:a {:href (str (config :modernator-url) "confirm-list/" auth-token)}
       "send invites to your list and start using modernator!"]]
     [:p "If you didn't sign up for this, sorry about that, looks like you got trolled."]
     [:p "Happy Collaborating,"]
     [:p "MojoTech"]]))

(defn user-welcome-email [auth-token]
  (page/html5
    [:body
     [:p "Hey There!"]
     [:p "You were added to a group to collaborate... on something..."]
     [:p "So why don't you go ahead and "
      [:a {:href (str (config :modernator-url) "confirm-user/" auth-token)}
       "start collaborating"]
      "."]
     [:p "Happy Collaborating,"]
     [:p "MojoTech"]]))
