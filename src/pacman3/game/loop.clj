(ns pacman3.game.loop
    (:require
      [pacman3.game.state :as gs]
      [pacman3.game.logic :as logic]
      [pacman3.ws :as ws]))

(defonce running? (atom false))
(defonce loop-handle (atom nil))

(def tick-ms 100)

(defn step! []
      ;; Все изменения мира — в dosync
      (let [state-after
            (dosync
              (alter gs/game-state logic/step-game)
              @gs/game-state)]
           (ws/broadcast-state! state-after)))

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
                                     ;; чтобы не падал поток
                                     nil)))))))

(defn stop! []
      (reset! running? false)
      (when-let [f @loop-handle]
                (future-cancel f)
                (reset! loop-handle nil)))
