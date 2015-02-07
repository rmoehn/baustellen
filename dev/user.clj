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

(def algo-params
  {; maximum number of "good" agents to consider during the neighborhood
   ; generation
   :n-good-agents 3

   ; number of iterations after which the tabu search shall terminate
   :n-iterations 100

   ; number of iterations a previously visit shall not be visited again
   :n-tabued 10
   })

(defn empty-distribution [static-data]
  {:allocation {} :reservoir (it/generate-reservoir static-data)})

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
