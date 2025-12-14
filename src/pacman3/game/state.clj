(ns pacman3.game.state
  (:require [clojure.string :as str])
  (:import (java.util UUID)))

(defonce game-state
  (ref {:players {}    ;; player-id -> {:id :nickname :role}
        :status  :waiting
        :winner  nil}))

(def roles-order [:pacman :ghost1 :ghost2])

(defn- normalize-nickname [s]
  (let [s (or s "Anon")
        s (str/trim s)]
    (if (str/blank? s) "Anon" s)))

(defn snapshot []
  @game-state)

(defn join! [{:keys [nickname]}]
  (let [nickname (normalize-nickname nickname)
        id (str (UUID/randomUUID))]
    (dosync
      (let [gs @game-state
            existing-roles (->> (vals (:players gs)) (map :role) set)
            role (or (some #(when-not (existing-roles %) %) roles-order)
                     :spectator)
            player {:id id :nickname nickname :role role}
            gs' (-> gs
                    (assoc-in [:players id] player)
                    (assoc :status (if (>= (count (keys (:players gs))) 1)
                                     :ready
                                     :waiting)))]
        (ref-set game-state gs')
        {:player player
         :state  gs'}))))

(defn leave! [player-id]
  (dosync
    (let [gs @game-state
          gs' (update gs :players dissoc player-id)
          gs'' (assoc gs' :status (if (empty? (:players gs'))
                                    :waiting
                                    (:status gs')))]
      (ref-set game-state gs'')
      gs'')))
