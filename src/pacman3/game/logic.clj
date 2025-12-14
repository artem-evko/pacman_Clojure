(ns pacman3.game.logic
    (:require
      [pacman3.game.level :as level]
      [pacman3.game.state :as st]))

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

(defn- active-player? [p]
       (and p (contains? (set st/active-roles) (:role p)) (:pos p)))

(defn- pacman-id+pos [players]
       (some (fn [[id p]] (when (= :pacman (:role p)) [id (:pos p)])) players))

(defn- ghost-poses [players]
       (->> players
            vals
            (filter #(or (= :ghost1 (:role %)) (= :ghost2 (:role %))))
            (map :pos)
            (remove nil?)
            set))

(defn step-game [gs]
      (cond
        (not= :running (:status gs)) gs

        :else
        (let [players (:players gs)
              ;; 1) двигаем активных
              players' (into {}
                             (map (fn [[id p]]
                                      (if (active-player? p)
                                        (let [pos' (move-one (:pos p) (:dir p))]
                                             [id (assoc p :pos pos')])
                                        [id p])))
                             players)
              ;; 2) столкновение
              [pac-id pac-pos] (pacman-id+pos players')
              gposes (ghost-poses players')
              caught? (and pac-pos (contains? gposes pac-pos))

              ;; 3) сбор точек
              dots (:dots gs)
              ate-dot? (and pac-pos (contains? dots pac-pos))
              dots' (if ate-dot? (disj dots pac-pos) dots)
              players'' (if (and pac-id ate-dot?)
                          (update-in players' [pac-id :score] (fnil inc 0))
                          players')

              pac-wins? (empty? dots')]

             (cond
               caught?
               (-> gs
                   (assoc :players players')
                   (assoc :dots dots)
                   (assoc :status :over)
                   (assoc :winner :ghosts))

               pac-wins?
               (-> gs
                   (assoc :players players'')
                   (assoc :dots dots')
                   (assoc :status :over)
                   (assoc :winner :pacman))

               :else
               (-> gs
                   (assoc :players players'')
                   (assoc :dots dots'))))))
