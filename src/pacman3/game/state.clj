(ns pacman3.game.state
  (:require
    [clojure.string :as str]
    [pacman3.game.level :as level])
  (:import (java.util UUID)))

(defonce game-state
  (ref {:players {}     ;; id -> {:id :nickname :role :pos [x y] :dir :left}
        :status  :waiting  ;; :waiting | :running
        :winner  nil}))

(def roles-order [:pacman :ghost1 :ghost2])

(defn- normalize-nickname [s]
  (let [s (or s "Anon")
        s (str/trim s)]
    (if (str/blank? s) "Anon" s)))

(defn snapshot [] @game-state)

(defn- assign-role [players]
  (let [used (->> (vals players) (map :role) set)]
    (or (some #(when-not (used %) %) roles-order)
        :spectator)))

(defn- start-pos [role]
  (get level/start-positions role))

(defn join! [{:keys [nickname]}]
  (let [nickname (normalize-nickname nickname)
        id (str (UUID/randomUUID))]
    (dosync
      (let [gs @game-state
            role (assign-role (:players gs))
            player (cond-> {:id id :nickname nickname :role role}
                     (not= role :spectator)
                     (assoc :pos (start-pos role)
                            :dir :left))
            gs' (assoc-in gs [:players id] player)
            active (count (filter #(not= :spectator (:role %)) (vals (:players gs'))))
            gs'' (assoc gs' :status (if (>= active 2) :running :waiting)
                           :winner nil)]
        (ref-set game-state gs'')
        {:player player
         :state  gs''}))))

(defn leave! [player-id]
  (dosync
    (let [gs @game-state
          gs' (update gs :players dissoc player-id)
          active (count (filter #(not= :spectator (:role %)) (vals (:players gs'))))
          gs'' (assoc gs' :status (if (>= active 2) :running :waiting)
                         :winner nil)]
      (ref-set game-state gs'')
      gs'')))

(defn change-dir! [player-id dir-keyword]
  (dosync
    (when (contains? #{:up :down :left :right} dir-keyword)
      (alter game-state assoc-in [:players player-id :dir] dir-keyword))
    @game-state))
