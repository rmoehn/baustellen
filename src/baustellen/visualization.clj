(ns baustellen.visualization
  (:require [rhizome.viz :as rh]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [baustellen :refer :all]
            [clojure.pprint :refer [pprint]]
            [net.cgrand.enlive-html :as html]))

(def green "#00aa77")

(defn providers [site-k allocation]
  (for [bundles (vals (allocation site-k))
        agent-k (keys bundles)]
    agent-k))

(defn loc->str [[x y]]
  (str (* x 0.5) \, (* y 0.5) \! ))

(defn entities [static-data]
  (merge (:agents static-data) (:sites static-data)))

(defn site? [k static-data]
  ((set (keys (:sites static-data))) k))

(defn shorten [s]
  (s/replace s #"(.).*?(\d+)" "$1$2"))

(defmulti format-node (fn [k _ static-data]
                        (if (site? k static-data) :site :agent)))

(defmethod format-node :site [k {:keys [allocation]} static-data]
  {:label (shorten (name k))
   :pos (loc->str (:location (get-in static-data [:sites k])))
   :shape "box"
   :style "filled"
   :color (if (demand-met? [k (allocation k)] static-data) green "red")})

(defmethod format-node :agent [k {:keys [reservoir]} static-data]
  (let [skill (get-in static-data [:agents k :skill])]
    {:label (shorten (name k))
     :pos (loc->str (:location (get-in static-data [:agents k])))
     :shape "circle"
     :style "filled"
     :fillcolor "gray"
     :penwidth 3
     :color (if (pos? (get-in reservoir [skill k])) green "red")}))

(defn format-edge [site-k agent-k allocation static-data]
  (let [skill (get-in static-data [:agents agent-k :skill])]
    {:penwidth (get-in allocation [site-k skill agent-k])}))

(defn make-image [{:keys [allocation reservoir] :as distr} static-data]
  (rh/graph->image
    (concat (keys (:sites static-data)) (keys (:agents static-data)))
              (fn [k] (and (site? k static-data) (providers k allocation)))
              :directed? false
              :vertical? false
              :options {:layout "fdp"
                        :splines "true"
                        :sep "0.5,0.5"
                        :dpi 60
                        }
              :node->descriptor #(format-node % distr static-data)
              :edge->descriptor #(format-edge %1 %2 allocation static-data)))

(defn show-allocation [& args]
  (rh/view-image (apply make-image args)))

(html/deftemplate before-after (io/resource "before-after.html")
  [basename]
  [:head :title] (html/content basename)
  [:#before]  (html/set-attr :src (str basename "-before.png"))
  [:#after]  (html/set-attr :src (str basename "-after.png")))

(defn save-before-after [distr-before distr-after static-data basename]
  (rh/save-image (make-image distr-before static-data)
                 (str basename "-before.png"))
  (rh/save-image (make-image distr-after static-data)
                 (str basename "-after.png"))
  (spit (str basename ".html") (apply str (before-after basename))))
