(ns pacman3.game.state
  (:require
    [clojure.string :as str]
    [pacman3.game.level :as level])
  (:import (java.util UUID)))

(defonce game-state
  (ref {:players {}       ;; id -> {:id :nickname :role :pos [x y] :dir :left :score 0}
        :dots    level/dots
        :status  :waiting  ;; :waiting | :running | :over
        :winner  nil}))

(def active-roles [:pacman :ghost1 :ghost2])

(defn snapshot [] @game-state)

(defn- normalize-nickname [s]
  (let [s (or s "Anon")
        s (str/trim s)]
    (if (str/blank? s) "Anon" s)))

(defn- active-player? [p]
  (and p (contains? (set active-roles) (:role p))))

(defn- active-count [players]
  (count (filter active-player? (vals players))))

(defn- used-roles [players]
  (->> (vals players) (map :role) set))

(defn- assign-role [players]
  (let [used (used-roles players)]
    (or (some #(when-not (used %) %) active-roles)
        :spectator)))

(defn- start-pos [role]
  (get level/start-positions role))

(defn- init-active-player [id nickname role]
  {:id id
   :nickname nickname
   :role role
   :pos (start-pos role)
   :dir :left
   :score 0})

(defn- recompute-status [gs]
  (let [players (:players gs)
        active (active-count players)]
    (cond
      (< active 3) (assoc gs :status :waiting :winner nil)
      (and (= active 3) (= :waiting (:status gs)))
      ;; если только что добрали 3-х, то стартуем новый раунд
      (-> gs
          (assoc :status :running :winner nil)
          (assoc :dots level/dots)
          (update :players
                  (fn [m]
                    (into {}
                          (map (fn [[id p]]
                                 (if (active-player? p)
                                   [id (assoc p
                                              :pos (start-pos (:role p))
                                              :dir :left
                                              :score 0)]
                                   [id p])))
                          m))))
      :else gs)))

(defn join! [{:keys [nickname]}]
  (let [nickname (normalize-nickname nickname)
        id (str (UUID/randomUUID))]
    (dosync
      (let [gs @game-state
            role (assign-role (:players gs))
            p (if (= role :spectator)
                {:id id :nickname nickname :role :spectator}
                (init-active-player id nickname role))
            gs' (assoc-in gs [:players id] p)
            gs'' (recompute-status gs')]
        (ref-set game-state gs'')
        {:player p :state gs''}))))

(defn leave! [player-id]
  (dosync
    (let [gs @game-state
          gs' (update gs :players dissoc player-id)
          gs'' (recompute-status gs')]
      (ref-set game-state gs'')
      gs'')))

(defn change-dir! [player-id dir-keyword]
  (dosync
    (when (contains? #{:up :down :left :right} dir-keyword)
      (let [p (get-in @game-state [:players player-id])]
        ;; направлением управляют только активные игроки
        (when (active-player? p)
          (alter game-state assoc-in [:players player-id :dir] dir-keyword))))
    @game-state))

(defn restart! []
  (dosync
    (let [gs @game-state
          players (:players gs)]
      (if (= 3 (active-count players))
        (let [gs' (-> gs
                      (assoc :dots level/dots)
                      (assoc :status :running)
                      (assoc :winner nil)
                      (update :players
                              (fn [m]
                                (into {}
                                      (map (fn [[id p]]
                                             (if (active-player? p)
                                               [id (assoc p
                                                          :pos (start-pos (:role p))
                                                          :dir :left
                                                          :score 0)]
                                               [id p])))
                                      m))))]
          (ref-set game-state gs')
          gs')
        ;; если не 3 активных — просто вернуть текущее
        gs))))
