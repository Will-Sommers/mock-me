(ns mock-me.core
  (:require-macros [cljs.core.async.macros :refer [go alt!]])
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ankha.core :as ankha]
            [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

(def app-state (atom {:views [
                              {:user {:type "admin" :name "Will Sommers"}}
                              {:user {:type "normal" :name "Yngwie Malmsteen"}}
                              {:user 1}]}))

(println "tes")


(def local-dragging? (atom false))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type (fn [event]
                             (when @local-dragging?
                               (.preventDefault event))
                             (put! out event)))
    out))

(defn draggable-window [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:c-mouse (chan)})
    om/IWillMount
    (will-mount [_]
      (let [c-mouse (om/get-state owner :c-mouse)
            mouse-move-chan (async/map
                             (fn [e] [(.-clientX e) (.-clientY e)])
                             [(listen js/window "mousemove")])
            mouse-up-chan (async/map
                             (fn [e] [(.-clientX e) (.-clientY e)])
                             [(listen js/window "mouseup")])]
        (go (while true
              (let [pos (<! c-mouse)]
                (.log js/console pos))))))
    om/IRenderState
    (render-state [_ {:keys [c-mouse]}]
      (dom/div #js {:onMouseDown #(do
                                    (reset! local-dragging? true)
                                    (put! c-mouse [(.-clientX %) (.-clientY %)]))} "Hi"))))

(defn app [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build draggable-window {:inspector ankha/inspector
                                           :content-data data})
               ))))

(defn init [app-state]
  (om/root
   app
   app-state
   {:target (. js/document (getElementById "app"))}))

(init app-state)
