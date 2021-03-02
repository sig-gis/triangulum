(ns triangulum.cli
  (:require [clojure.string    :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [triangulum.utils  :refer [format-%]]))

(defn- single-option [required cli-options]
  (let [[shrt lng _] (required cli-options)]
    (format-% "%l (%s)" lng shrt)))

(defn- error-str [action requires cli-options]
  (format-% "The action %a requires %r."
            (name action)
            (case (count requires)
              1 (single-option (first requires) cli-options)

              2 (str (single-option (first requires) cli-options)
                     " and "
                     (single-option (second requires) cli-options))

              (str (->> (butlast requires)
                        (map #(single-option % cli-options))
                        (str/join ", "))
                   ", and "
                   (single-option (last requires) cli-options)))))

(defn- usage-str [cli-options cli-actions alias-str]
  (let [option-str (map (fn [[_ [shrt lng description]]]
                          (format "  %s, %-23s%s" shrt lng description))
                        cli-options)
        action-str (map (fn [[action info]]
                          (format "   %-26s%s" (name action) (:description info)))
                        cli-actions)]
    (->> (concat [(str "Usage: clojure -M:" alias-str " [options] action")
                  ""
                  "Options:"]
                 option-str
                 [""
                  "Actions:"]
                 action-str)
         (str/join "\n"))))

(defn- check-errors [arguments errors options action cli-options cli-actions]
  (let [requires (get-in cli-actions [action :requires])]
    (cond
      (seq errors)
      (str/join "\n" errors)

      (or (nil? cli-actions)
          (= 0 (count cli-actions)))
      false

      (= 0 (count arguments))
      "You must select an action."

      (< 1 (count arguments))
      "You only select one action at a time."

      (nil? requires)
      (str "\"" (name action) "\" is not a valid action.")

      (not-every? options requires)
      (error-str action requires cli-options))))

(defn get-cli-options [args cli-options cli-actions alias-str]
  (let [{:keys [arguments errors options]} (->> cli-options
                                                (map second)
                                                (parse-opts args))
        action    (keyword (first arguments))
        error-msg (check-errors arguments errors options action cli-options cli-actions)]
    (if error-msg
      (do
        (println "Error:" error-msg "\n")
        (println (usage-str cli-options cli-actions alias-str)))
      {:options options
       :action  action})))
