package com.guokr.nlp.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.guokr.nlp.SegWrapper;
import com.guokr.nlp.NerWrapper;
import com.guokr.nlp.TagWrapper;

@RunWith(JUnit4.class)
public class BasicTests {

    @Test
    public void testSeg() {
        assertEquals("这 是 个 测试", SegWrapper.segment("这是个测试"));
    }

}
