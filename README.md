stan-cn-nlp
============

An API wrapper based on Stanford NLP packages for the convenience of Chinese
users. This package is based on stan-cn-* family:

* stan-cn-com: Common code base
* stan-cn-seg: Chinese segmentation and related data model
* stan-cn-ner: Named entity recognition and related data model
* stan-cn-tag: POS tagging and related data model

This package bundled seg, ner and tagging together. So if you only need one of
them, you can use stan-cn-seg, stan-cn-ner, stan-cn-tag separately.

Purpose of the packages
------------------------

The original Stanford CoreNLP packages with default language settings in Maven
central is only for English. If you are dealing with simplified Chinese, you
still need to download the Chinese model and fix some configuration files.

The burden is not too much, but if you deploy these packages to a server
cluster, this burden might be amplified.

Whatever you face a single node or a server farm, it would be a pleasurable
solution to provide packages with default settings of Chinese language
models. That is what we do.

Comments, reviews, bug reports and patches are welcomed.

Current version
----------------

Current version is 0.0.4 and based on Stanford CoreNLP 3.2.0 with minor fixes.

including below dependency:

* maven:
```xml
    <dependency>
        <groupId>com.guokr</groupId>
        <artifactId>stan-cn-nlp</artifactId>
        <version>0.0.4</version>
    </dependency>
```
* leiningen:
```clojure
    [com.guokr/stan-cn-nlp "0.0.4"]
```
* sbt:
```scala
    libraryDependencies += "com.guokr" % "stan-cn-nlp" % "0.0.4"
```

Simplified API
---------------

We use a very simple API to reduce the complexity.

```java
    new SegWrapper(settings).segment(text);
    new NerWrapper(settings).recognize(text);
    new TagWrapper(settings).tag(text);
```

Or if you want to use the default language models, just use

```java
    __PKG__.INSTANCE.segment(text);
    __PKG__.INSTANCE.recognize(text);
    __PKG__.INSTANCE.tag(text);
```

The command line tool
----------------------

Please follow below steps to play with:

* git clone git://github.com/guokr/stan-cn-nlp.git
* cd stan-cn-nlp
* mvn package
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar seg "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar ner "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar tag "大江东去浪淘尽"

Preparation for release
------------------------

Before release this package to maven central, please execute below commands:

* mvn clean source:jar javadoc:jar package
* export MAVEN_OPTS=-Xmx2048m
* mvn release:clean
* mvn release:prepare
* mvn release:perform

Authors
--------

* Mingli Yuan ( https://github.com/mountain )
* Rui Wang ( https://github.com/isnowfy )
* Wanjian Wu ( https://github.com/jseagull )

License
--------

GPLv2, just same as the license of Stanford CoreNLP package
