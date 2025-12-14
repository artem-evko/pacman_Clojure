(ns pacman3.game.state
    (:require
      [clojure.string :as str]
      [pacman3.game.level :as level])
    (:import (java.util UUID)))

(defonce game-state
         (ref {:players {}      ;; id -> {:id :nickname :role :pos [x y] :dir :left :score 0}
               :dots    #{}     ;; #{[x y] ...}
               :status  :waiting ;; :waiting | :running | :over
               :winner  nil}))   ;; :pacman | :ghosts | nil

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

(defn- active-player? [p]
       (and p (not= :spectator (:role p))))

(defn- active-count [players]
       (count (filter active-player? (vals players))))

(defn- initial-player [id nickname role]
       (cond-> {:id id :nickname nickname :role role}
               (not= role :spectator)
               (assoc :pos (start-pos role)
                      :dir :left
                      :score 0)))

(defn join! [{:keys [nickname]}]
      (let [nickname (normalize-nickname nickname)
            id (str (UUID/randomUUID))]
           (dosync
             (let [gs @game-state
                   role (assign-role (:players gs))
                   p (initial-player id nickname role)
                   gs' (assoc-in gs [:players id] p)
                   active (active-count (:players gs'))
                   gs'' (-> gs'
                            (assoc :status (if (>= active 2) :running :waiting))
                            (assoc :winner nil))]
                  (ref-set game-state gs'')
                  {:player p :state gs''}))))

(defn leave! [player-id]
      (dosync
        (let [gs @game-state
              gs' (update gs :players dissoc player-id)
              active (active-count (:players gs'))
              gs'' (-> gs'
                       (assoc :status (cond
                                        (empty? (:players gs')) :waiting
                                        (>= active 2) :running
                                        :else :waiting))
                       (assoc :winner nil))]
             (ref-set game-state gs'')
             gs'')))

(defn change-dir! [player-id dir-keyword]
      (dosync
        (when (contains? #{:up :down :left :right} dir-keyword)
              (alter game-state assoc-in [:players player-id :dir] dir-keyword))
        @game-state))

(defn restart! []
      (dosync
        (let [gs @game-state
              players (:players gs)
              ;; на рестарте: сохраняем состав игроков и роли, но сбрасываем позицию/очки для активных
              players' (into {}
                             (map (fn [[id p]]
                                      [id (if (active-player? p)
                                            (-> p
                                                (assoc :pos (start-pos (:role p)))
                                                (assoc :dir :left)
                                                (assoc :score 0))
                                            p)]))
                             players)
              active (active-count players')
              gs' (-> gs
                      (assoc :players players')
                      (assoc :dots level/dots) ;; всё заново
                      (assoc :status (if (>= active 2) :running :waiting))
                      (assoc :winner nil))]
             (ref-set game-state gs')
             gs')))
