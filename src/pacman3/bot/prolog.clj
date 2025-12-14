(ns pacman3.bot.prolog
  (:require
    [clojure.java.shell :as sh]
    [clojure.string :as str]))

(defn choose-dir
  "Возвращает одно из: :up :down :left :right или nil.
   ghost-pos/pacman-pos: [x y]
   legal-dirs: например [:up :left :right]
   Требует установленный SWI-Prolog: команда `swipl` в PATH."
  [{:keys [ghost-pos pacman-pos legal-dirs]}]
  (let [[xg yg] ghost-pos
        [xp yp] pacman-pos
        input (format "state(ghost(%d,%d), pacman(%d,%d), moves([%s]))."
                      xg yg xp yp (str/join "," (map name legal-dirs)))
        {:keys [out err]} (sh/sh "swipl" "-q" "-s" "prolog/ghost_ai.pl" :in input)]
    (when-not (seq err)
      (case (str/trim out)
        "move(up)." :up
        "move(down)." :down
        "move(left)." :left
        "move(right)." :right
        nil))))
