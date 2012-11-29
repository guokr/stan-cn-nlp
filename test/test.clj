(ns test
  (:use clojure.test)
  (:require [com.guokr.stancnnlp.seg :as seg])
  (:require [com.guokr.stancnnlp.tag :as tag])
  (:require [com.guokr.stancnnlp.ner :as ner]))

(deftest mytest []
  (println (seg/seg "这是个测试"))
  (println (tag/tag "这是个测试"))
  (println (ner/ner "这是个测试")))
