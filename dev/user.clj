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
    [clojure.walk :as walk]
    [clojure.data :refer [diff]]
    [baustellen :refer :all]
    [baustellen.input-transformation :as it]
    [baustellen.visualization :as viz]))

(def skill-cost {:walls 30
                 :roof 20
                 :plumbing 10})

(def sites (it/pull-in-names {:sporthalle
                              {:payoff 1400
                               :skills {:walls 3
                                        :roof 6
                                        :plumbing 1}
                               :location [4 4]}

                              :schwimmhalle
                              {:payoff 1600
                               :skills {:walls 2
                                        :roof 0
                                        :plumbing 8}
                               :location [3 1]}

                              :einfamilienhaus
                              {:payoff 1150
                               :skills {:walls 5
                                        :roof 5
                                        :plumbing 5}
                               :location [7 1]}}))

(def agents (it/pull-in-names
              {:maurer1     {:skill :walls
                             :capacity 10
                             :location [1 5]}
               :maurer2     {:skill :walls
                             :capacity 7
                             :location [5 4]}
               :dachdecker1 {:skill :roof
                             :capacity 15
                             :location [0 2]}
               :klempner1   {:skill :plumbing
                             :capacity 8
                             :location [3 2]}
               :klempner2   {:skill :plumbing
                             :capacity 8
                             :location [8 2]}}))

(def coalition {:site :sporthalle
                :allocation {:walls {:maurer1 1 :maurer2 2}
                             :roof {:dachdecker1 6}
                             :plumbing {:klempner1 1}}})

(def static-data {:sites sites
                  :agents agents
                  :skill-cost skill-cost})

(def completed-coalition
  (->> coalition
       (walk/prewalk-replace agents)
       (walk/prewalk-replace sites)))

(def algo-params
  {; maximum number of "good" agents to consider during the neighborhood
   ; generation
   :n-good-agents 3

   ; number of iterations after which the tabu search shall terminate
   :n-iterations 100

   ; number of iterations a previously visit shall not be visited again
   :n-tabued 10
   })

(def initial-distribution
  (find-initial-distribution (it/generate-reservoir static-data) static-data))

(def ex-distr {:allocation
               {:einfamilienhaus
                {:roof  {:dachdecker1 3},
                 :walls  {:maurer1 3, :maurer2 2},
                 :plumbing  {:klempner2 5}},
                :schwimmhalle
                {:walls  {:maurer1 0, :maurer2 2},
                 :plumbing  {:klempner2 1, :klempner1 7}},
                :sporthalle
                {:roof  {:dachdecker1 6},
                 :walls  {:maurer1 0, :maurer2 3},
                 :plumbing  {:klempner2 0, :klempner1 1}}},
               :reservoir
               {:plumbing  {:klempner1 0, :klempner2 2},
                :roof  {:dachdecker1 6},
                :walls  {:maurer1 7, :maurer2 0}}})
