(ns pacman3.game.loop
  (:require
    [pacman3.game.state :as gs]
    [pacman3.game.logic :as logic]
    [pacman3.game.level :as level]
    [pacman3.bot.prolog :as prolog]
    [pacman3.ws :as ws]))

(defonce running? (atom false))
(defonce loop-handle (atom nil))

(def tick-ms 120)

(defn- legal-dirs [[x y]]
  (->> [[:up [0 -1]] [:down [0 1]] [:left [-1 0]] [:right [1 0]]]
       (keep (fn [[d [dx dy]]]
               (let [p [(+ x dx) (+ y dy)]]
                 (when-not (level/wall? p) d))))))

(defn- bot-ghost? [p]
  (and (:is-bot p)
       (contains? #{:ghost1 :ghost2} (:role p))
       (:pos p)))

(defn step! []
  ;; 1) читаем снапшот
  (let [st (gs/snapshot)
        players (:players st)
        pac-pos (some (fn [[_ p]] (when (= :pacman (:role p)) (:pos p))) players)]
    ;; 2) если есть pacman — ботам меняем dir через prolog
    (when pac-pos
      (doseq [[id p] players]
        (when (bot-ghost? p)
          (let [dirs (legal-dirs (:pos p))
                dirs (if (seq dirs) dirs [:left])
                dir (prolog/choose-dir {:ghost-pos (:pos p)
                                        :pacman-pos pac-pos
                                        :legal-dirs dirs})]
            (when dir
              (gs/change-dir! id dir))))))

    ;; 3) обычный шаг игры в STM
    (let [state-after
          (dosync
            (alter gs/game-state logic/step-game)
            @gs/game-state)]
      (ws/broadcast-state! state-after))))

(defn start! []
  (when-not @running?
    (reset! running? true)
    (reset! loop-handle
            (future
              (while @running?
                (Thread/sleep tick-ms)
                (try
                  (step!)
                  (catch Exception _e
                    nil)))))))

(defn stop! []
  (reset! running? false)
  (when-let [f @loop-handle]
    (future-cancel f)
    (reset! loop-handle nil)))
