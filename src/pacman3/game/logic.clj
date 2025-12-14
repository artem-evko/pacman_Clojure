(ns pacman3.game.logic
    (:require
      [pacman3.game.level :as level]))

(def dir->delta
  {:up [0 -1]
   :down [0 1]
   :left [-1 0]
   :right [1 0]})

(defn- add-pos [[x y] [dx dy]]
       [(+ x dx) (+ y dy)])

(defn- move-one [pos dir]
       (let [delta (get dir->delta dir [0 0])
             next (add-pos pos delta)]
            (if (level/wall? next) pos next)))

(defn step-game [gs]
      (if (not= :running (:status gs))
        gs
        (let [players (:players gs)
              ;; двигаем только активных
              players' (into {}
                             (map (fn [[id p]]
                                      (if (or (= :spectator (:role p)) (nil? (:pos p)))
                                        [id p]
                                        (let [pos' (move-one (:pos p) (:dir p))]
                                             [id (assoc p :pos pos')]))))
                             players)
              pacman-pos (some (fn [[_ p]] (when (= :pacman (:role p)) (:pos p))) players')
              ghost-poses (->> players'
                               vals
                               (filter #(or (= :ghost1 (:role %)) (= :ghost2 (:role %))))
                               (map :pos)
                               (remove nil?)
                               set)
              caught? (and pacman-pos (contains? ghost-poses pacman-pos))]
             (-> gs
                 (assoc :players players')
                 (cond-> caught?
                         (assoc :status :waiting
                                :winner :ghosts))))))
