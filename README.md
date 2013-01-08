stan-cn-nlp
===========

An API wrapper based on Stanford NLP packages for the convenience of Chinese users.

This package is at its early stage (v0.0.1), bugs may exists, we recommend you
not to use it for production right now.

Comments, reviews, bug reports and patches are welcomed.

How to play
------------

Please follow below steps to play with:

* git clone git://github.com/guokr/stan-cn-nlp.git
* cd stan-cn-nlp
* mvn package
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar seg "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar ner "大江东去浪淘尽"
* java -Xms1g -Xmx2g -jar target/stan-cn-nlp-0.0.3-SNAPSHOT-standalone.jar tag "大江东去浪淘尽"

The API
--------

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

Preparation for release
------------------------

Before release this package to maven central, please execute below commands:

* mvn clean source:jar javadoc:jar package

License
--------

GPLv2, just as Stanford CoreNLP package

