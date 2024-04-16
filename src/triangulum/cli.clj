(ns triangulum.cli
  "Provides a command-line interface (CLI) for Triangulum applications. It includes functions for parsing command-line options, displaying usage information, and checking for errors in the provided arguments."
  (:require [clojure.string    :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [triangulum.utils  :refer [format-str]]))

(defn- single-option [required cli-options]
  (let [[shrt lng _] (required cli-options)]
    (format-str "%l (%s)" lng shrt)))

(defn- error-str [action requires cli-options]
  (format-str "The action %a requires %r."
              (name action)
              (case (count requires)
                1 (single-option (first requires) cli-options)
                2 (str/join " and " (map #(single-option % cli-options) requires))
                (str (->> (butlast requires)
                          (map #(single-option % cli-options))
                          (str/join ", "))
                     ", and "
                     (single-option (last requires) cli-options)))))

(defn- usage-str [cli-options cli-actions alias-str]
  (let [options (map (fn [[shrt lng description]]
                       (format "  %s, %-23s%s" shrt lng description))
                     (vals cli-options))
        actions (map (fn [[action info]]
                       (format "   %-26s%s" (name action) (:description info)))
                     cli-actions)]
    (->> (concat [(str "Usage: `clojure -M:" alias-str " action [options]`.")
                  "Note that all options can also be passed into get-cli-options as config."
                  ""
                  "Actions:"]
                 actions
                 [""
                  "Options:"]
                 options)
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
      "You can only select one action at a time."

      (not (contains? cli-actions action))
      (str "\"" (name action) "\" is not a valid action.")

      (not-every? options requires)
      (error-str action requires cli-options))))

(defn- get-option-default [option]
  (loop [cur     (first option)
         tail    (next option)
         result  []]
    (cond
      (nil? cur)
      {:option result :default nil}

      (= :default cur)
      {:option  (vec (concat result (next tail)))
       :default (first tail)}

      :else
      (recur (first tail)
             (next tail)
             (conj result cur)))))

(defn- separate-options-defaults [options]
  (reduce (fn [acc [k v]]
            (let [{:keys [option default]} (get-option-default v)]
              (-> acc
                  (assoc-in [:options k] option)
                  (assoc-in [:defaults k] default))))
          {:options {} :defaults {}}
          options))

(defn get-cli-options
  "Checks for a valid call from the CLI and returns the users options.

   Takes the command-line arguments, a map of CLI options, a map of CLI actions,
   an alias string, and an optional config map.

   Example:
   ```clojure
   (def cli-options {...})

   (def cli-actions {...})
   (def alias-str \"...\")

   (get-cli-options command-line-args cli-options cli-actions alias-str)
   ```"
  [args cli-options cli-actions alias-str & [config]]
  (let [{:keys [options defaults]} (separate-options-defaults cli-options)
        {:keys [arguments errors options]} (->> options
                                                (vals)
                                                (parse-opts args))
        combined-options (merge defaults config options) ; defaults, config file, then cli params can overwrite.
        action           (keyword (first arguments))
        error-msg        (check-errors arguments errors combined-options action cli-options cli-actions)]
    (if error-msg
      (do
        (println "Error:" error-msg "\n")
        (println (usage-str cli-options cli-actions alias-str)))
      {:options combined-options
       :action  action})))
