(defproject stan-cn-nlp "0.1.0-SNAPSHOT"
            :description "FIXME: write this!"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3"]]
            :java-source-paths ["src/java"]
            :clojure-source-paths ["src/clojure"]
            :main com.guokr.stancnnlp.server
            :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
            :jvm-opts ["-Xmx1g"])
