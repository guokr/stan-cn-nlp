package com.guokr.nlp.test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.guokr.nlp.__PKG__;

@RunWith(JUnit4.class)
public class BasicTests {

    @Test
    public void testSeg() throws Exception {
        String segText = __PKG__.INSTANCE.segment("这是个测试");
        assertEquals("这 是 个 测试", segText);
    }

    @Test
    public void testNer() throws Exception {
        String nerText = __PKG__.INSTANCE.recognize("这是个测试");
        assertEquals("这/O 是/O 个/O 测试/O", nerText);
    }

    @Test
    public void testTag() throws Exception {
        String tagText = __PKG__.INSTANCE.tag("这是个测试");
        assertEquals("这#PN 是#VC 个#M 测试#NN", tagText);
    }

}
