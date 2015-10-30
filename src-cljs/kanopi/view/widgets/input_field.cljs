(ns kanopi.view.widgets.input-field
  (:require [om.core :as om]
            [taoensso.timbre :as timbre
             :refer-macros (log trace debug info warn error fatal report)]
            [kanopi.view.icons :as icons]
            [kanopi.util.browser :as browser]
            [sablono.core :refer-macros [html] :include-macros true]
            ))

(defn- start-edit [e owner korks]
  (om/set-state! owner korks true))

(defn- handle-change [e owner korks]
  (om/set-state! owner korks (.. e -target -value)))

(defn- end-edit [e owner editing-state-korks handler-fn]
  (om/set-state! owner editing-state-korks false)
  (handler-fn (.. e -target -value))
  (om/set-state! owner :new-value nil))

(defn editable-value
  "Required initial state:
     edit-key for accessing value to be edited from props
     submit-value
  "
  [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false
       :edit-icon-enabled true
       :hovering false
       :default-value "Input field default value"})

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      ;; focus on text field when editing an input field
      (when (and (not (get prev-state :editing))
                 (om/get-state owner :editing))
       (. (om/get-node owner "text-field") (focus))))

    om/IRenderState
    (render-state [_ {:keys [editing edit-key submit-value hovering default-value]
                      :as state}]
      (let []
        (html
         [:span.editable-text-container
          [:span.view-editable-text
             {:style {:display (when editing "none")}
              :class [(when hovering "bold-text")]
              :on-click #(start-edit % owner :editing)}
             (if-let [current-value (not-empty (get props :edit-key))]
               current-value
               default-value)]

          [:input.edit-editable-text
           {:style       {:display (when-not editing "none")}
            :ref         "text-field"
            :type        "text"
            :value       (or (get state :new-value) (get props edit-key) default-value)
            :placeholder (get state :placeholder)
            :on-change   #(handle-change % owner :new-value)
            :on-key-down #(when (= (.-key %) "Enter")
                            ;; NOTE: this triggers on-blur.
                            (om/set-state! owner :editing false))
            :on-blur     #(end-edit % owner :editing submit-value)}]
          #_(when (get state :edit-icon-enabled)
              (icons/edit-in-place {:style {:display (when (or (not hovering)
                                                               editing)
                                                       "none")}
                                    :class "editable-text-icon"
                                    :on-click #(start-edit % owner :editing)}))

          ])))))

(defn input-field [props owner opts]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IRenderState
    (render-state [_ {:keys [submit-value] :as state}]
      (let []
        (html
         [:input.edit-editable-text
          {:ref         "text-field"
           :type        "text"
           :value       (get state :new-value)
           :placeholder (get state :placeholder)
           :on-change   #(handle-change % owner :new-value)
           :on-key-down (constantly nil)
           :on-blur     #(end-edit % owner :editing submit-value)
           }])))))

(defn textarea [props owner opts]
  (reify
    om/IRenderState
    (render-state [_ {:keys [submit-value] :as state}]
      (let []
        (html
         [:textarea.input-textarea
          {:value       (get state :new-value)
           :rows 3
           :cols 32
           :placeholder (get state :placeholder)
           :on-change   #(handle-change % owner :new-value)
           :on-key-down (constantly nil)
           :on-blur     #(end-edit % owner :editing submit-value)
           }
          ])))))
