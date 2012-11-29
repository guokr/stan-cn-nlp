(ns test
  (:use clojure.test)
  (:require [com.guokr.nlp.seg :as seg])
  (:require [com.guokr.nlp.tag :as tag])
  (:require [com.guokr.nlp.ner :as ner]))

(deftest mytest []
  (println (seg/seg "这是个测试"))
  (println (tag/tag "这是个测试"))
  (println (ner/ner "这是个测试")))
