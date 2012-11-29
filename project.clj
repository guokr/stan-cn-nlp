(defproject org.clojars.guokr/stan-cn-nlp "0.1.0-SNAPSHOT"
            :description "a clojure project based on Stanford NLP packages, only for convenience, one jar"
            :dependencies [[org.clojure/clojure "1.4.0"]]
            :java-source-paths ["src/java"]
            :source-paths ["src/clojure"]
            :test-paths ["test"]
            :main main
            :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
            :jvm-opts ["-Xmx1g"])
