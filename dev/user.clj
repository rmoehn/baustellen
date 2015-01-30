(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [baustellen :refer :all]))

(def skill-cost {:walls 30
                 :roof 20
                 :plumbing 10})

(def sites [{:payoff 300
             :name "Sporthalle"
             :skills {:walls 3
                      :roof 6
                      :plumbing 1}
             :location [4 4]}
            {:payoff 600
             :name "Schwimmhalle"
             :skills {:walls 2
                      :roof 0
                      :plumbing 8}
             :location [3 1]}
            {:payoff 150
             :name "Einfamilienhaus"
             :skills {:walls 5
                      :roof 5
                      :plumbing 5}
             :location [7 1]}])

(def agents [{:name "Maurer 1"
              :skill :walls
              :capacity 10
              :location [1 5]}
             {:name "Maurer 2"
              :skill :walls
              :capacity 7
              :location [5 4]}
             {:name "Dachdecker 1"
              :skill :roof
              :capacity 15
              :location [0 2]}
             {:name "Klempner 1"
              :skill :plumbing
              :capacity 8
              :location [3 2]}
             {:name "Klempner 2"
              :skill :plumbing
              :capacity 8
              :location [8 2]}])


