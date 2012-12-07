package com.guokr.nlp.test;

import static org.junit.Assert.assertTrue;

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
        String exp = "这 是 个 测试";
        String seg = SegWrapper.segment("这是个测试");
        boolean judge =  exp.equals(seg);
        System.err.println("results: " + judge + "\nexp:" + exp + "\nseg:" + seg);
        assertTrue(judge);
    }

}
