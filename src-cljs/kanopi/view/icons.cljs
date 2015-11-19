(ns kanopi.view.icons
  (:require [kanopi.util.browser :as browser]
            [kanopi.model.message :as msg]))

(defn open [m]
  [:div.glyphicon.glyphicon-chevron-right
   (merge {} m)])

(defn edit [m]
  [:div.glyphicon.glyphicon-pencil
   (merge {} m)])

(defn edit-in-place [m]
  [:div.glyphicon.glyphicon-pencil
   (merge {} m)])

(defn search [m]
  [:div.glyphicon.glyphicon-search
   (merge {} m)])

(defn log-in [m]
  [:div.glyphicon.glyphicon-log-in
   (merge {} m)])

(defn create [m]
  [:div.glyphicon.glyphicon-plus
   (merge {} m)])

;; FIXME: should be a light bulb
(defn insights [m]
  [:div.glyphicon.glyphicon-flash
   (merge {} m)])

(defn goal [m]
  [:div.glyphicon.glyphicon-pushpin
   (merge {} m)])

(defn user [m]
  [:div.glyphicon.glyphicon-user
   (merge {} m)])

;; TODO: build button wrapper function

(defn link-to
  "Example usage:
  (->> (icons/open {})
       (icons/link-to owner :settings {:class \"navbar-brand\"}))
  "
  ([owner route icon-markup]
   (link-to owner route {} icon-markup))

  ([owner route m icon-markup]
   (let [route (if (sequential? route) route (vector route))]
    [:a (merge m {:href (apply browser/route-for owner route)}) 
     icon-markup])))

(defn on-click
  ([click-fn icon-markup]
   (on-click click-fn {} icon-markup))

  ([click-fn m icon-markup]
   [:a (merge m {:on-click click-fn})
    icon-markup]))

(defn trigger-msg
  ([owner msg icon-markup]
   [:a {:on-click #(msg/send! owner msg)}
    icon-markup]))

(defn transform-scale
  ([& args]
   (let [{:keys [from to]
          :or {from 48, to 48}}
         (apply hash-map args)]
     (str "scale(" (/ to from) ")"))))

(defn svg-done
  "Google Material Design: action/done"
  [m]
  [:path
   (merge {:d "M18 32.34L9.66 24l-2.83 2.83L18 38l24-24-2.83-2.83z"}
          m)])

(defn svg-clear
  "Google Material Design: content/clear"
  [m]
  [:path
   (merge {:d "M38 12.83L35.17 10 24 21.17 12.83 10 10 12.83 21.17 24 10 35.17 12.83 38 24 26.83 35.17 38 38 35.17 26.83 24z"}
          m)])

(defn svg-restore
  "Google Material Design: action/restore"
  [m]
  [:path
   (merge {:d "M25.99 6C16.04 6 8 14.06 8 24H2l7.79 7.79.14.29L18 24h-6c0-7.73 6.27-14 14-14s14 6.27 14 14-6.27 14-14 14c-3.87 0-7.36-1.58-9.89-4.11l-2.83 2.83C16.53 39.98 21.02 42 25.99 42 35.94 42 44 33.94 44 24S35.94 6 25.99 6zM24 16v10l8.56 5.08L34 28.65l-7-4.15V16h-3z"}
          m)])

(comment
(defn edit
  "Google Material Design: "
  [m]
  [:path
   (merge {:d ""}
          m)]
  ;; <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48"><path d="M6 34.5V42h7.5l22.13-22.13-7.5-7.5L6 34.5zm35.41-20.41c.78-.78.78-2.05 0-2.83l-4.67-4.67c-.78-.78-2.05-.78-2.83 0l-3.66 3.66 7.5 7.5 3.66-3.66z"/></svg> 
  )
 )
