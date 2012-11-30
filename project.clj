(defproject org.clojars.guokr/stan-cn-nlp "0.1.0-SNAPSHOT"
    :description "a clojure project based on Stanford NLP packages, only for convenience, one jar"
    :dependencies [[org.clojure/clojure "1.4.0"] [edu.stanford.nlp/stanford-corenlp "1.3.3"]]
    :source-paths ["src" "src/main/clojure"]
    :java-source-paths ["src" "src/main/java"]
    :test-paths ["test" "src/test/clojure"]
    :resource-paths ["src/main/resources"] ; non-code files included in classpath/jar
    :compile-path "target/classes"   ; for .class files
    :target-path "target/"           ; where to place the project's jar file
    :jar-name "stan-cn-nlp.jar"           ; name of the jar produced by 'lein jar'
    :uberjar-name "stan-cn-nlp-standalone.jar" ; as above for uberjar
    :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
    :jvm-opts ["-Xmx2g"])
